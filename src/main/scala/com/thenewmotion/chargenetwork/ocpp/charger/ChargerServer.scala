package com.thenewmotion.chargenetwork
package ocpp.charger

import akka.actor.{Actor, Props, ActorRef}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.thenewmotion.ocpp.chargepoint.ChargePoint
import com.thenewmotion.ocpp.spray.OcppProcessing
import com.typesafe.scalalogging.slf4j.Logging
import _root_.spray.can.Http
import _root_.spray.http.{HttpResponse, HttpRequest}
import java.net.URI
import scala.concurrent.duration._
import scala.concurrent.Await

class ChargerServer(port: Int) {

  import ChargerServer._

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
      case req: HttpRequest => sender ! handleRequest(req)
    }

    def handleRequest(req: HttpRequest): HttpResponse = {
      val result = OcppProcessing[ChargePoint](req, x => map.get(x.chargerId))
      result match {
        case Left(error) => error
        case Right((chargerId, msg)) => msg()
      }
    }
  }
}

object ChargerServer extends Logging {
  case class Register(chargerId: String, cp: ChargePoint)
}
