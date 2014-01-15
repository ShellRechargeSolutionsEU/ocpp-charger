package com.thenewmotion.chargenetwork
package ocpp.charger

import akka.actor.{Actor, Props, ActorRef}
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.thenewmotion.ocpp.messages.chargepoint.{ChargePoint, Req => CpReq, Res => CpRes}
import com.thenewmotion.ocpp.spray.{ChargerInfo, OcppProcessing}
import com.typesafe.scalalogging.slf4j.Logging
import _root_.spray.can.Http
import _root_.spray.http.{HttpResponse, HttpRequest}
import java.net.URI
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Await, Future}

class ChargerServer(port: Int) {

  import ChargerServer._
  implicit val ec: ExecutionContext = system.dispatcher

  val actor: ActorRef = {
    implicit val timeout = Timeout(1 second)
    val actor = system.actorOf(Props(new ChargerServerActor))
    val future = IO(Http) ? Http.Bind(actor, interface = "localhost", port = port)
    Await.result(future, timeout.duration)
    actor
  }

  def url = new URI(s"http://localhost:$port")

  class ChargerServerActor extends Actor {
    var map: Map[String, ChargePoint] = Map()

    def receive = {
      case Register(chargerId, cp) => map = map + (chargerId -> cp)
      case Http.Connected(_, _) => sender ! Http.Register(self)
      case req: HttpRequest => handleRequest(req) pipeTo sender
    }

    def handleRequest(req: HttpRequest): Future[HttpResponse] = {
      OcppProcessing[CpReq, CpRes](req) {
        case (chargerInfo: ChargerInfo, cpReq: CpReq) => {
          map.get(chargerInfo.chargerId) match {
            case None => Future.failed(new NoSuchElementException(s"I am not charger ${chargerInfo.chargerId}"))
            case Some(chargePoint) => Future.successful(chargePoint(cpReq))
          }
        }
      }
    }
  }
}

object ChargerServer extends Logging {
  case class Register(chargerId: String, cp: ChargePoint)
}
