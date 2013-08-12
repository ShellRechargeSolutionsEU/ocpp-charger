package com.thenewmotion.chargenetwork
package ocpp.charger

import akka.actor.{Actor, Props, ActorRef}
import com.thenewmotion.ocpp.chargepoint.ChargePoint
import com.thenewmotion.ocpp.spray.OcppProcessing
import com.typesafe.scalalogging.slf4j.Logging
import akka.io.IO
import _root_.spray.can.Http
import _root_.spray.http.{HttpResponse, HttpRequest}
import java.net.URI

class ChargerServer(port: Int) {

  import ChargerServer._

  val actor: ActorRef = {
    val actor = system.actorOf(Props(new ChargerServerActor))
    IO(Http) ! Http.Bind(actor, interface = "localhost", port = port)
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
