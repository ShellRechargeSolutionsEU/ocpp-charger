package com.thenewmotion.chargenetwork
package ocpp.charger

import spray.can.server.SprayCanHttpServerApp
import java.net.URI
import akka.actor.{Actor, Props, ActorRef}
import spray.http.HttpRequest
import com.thenewmotion.ocpp._
import com.typesafe.scalalogging.slf4j.Logging
import com.thenewmotion.ocpp.spray.{OcppProcessing, ChargerInfo}


class ChargerHTTPServer(val listenPort: Int) extends Logging with SprayCanHttpServerApp {
  override lazy val system = com.thenewmotion.chargenetwork.ocpp.charger.system

  private val interface = "localhost"

  private class ChargerServerActor extends Actor {
    def receive = {
      case req: HttpRequest =>
        val result = OcppProcessing[ChargePointService](req, (_: ChargerInfo) =>  Some(LoggingChargePointService))
        result match {
          case Left(error) => sender ! error
          case Right((chargerId, msg)) => sender ! msg.apply
        }
    }
  }

  private val requestHandler = system.actorOf(Props(new ChargerServerActor))
  newHttpServer(requestHandler) ! Bind(interface = interface, port = listenPort)

  def listenURI: URI = new URI("http", null, interface, listenPort, null, null, null)

  def addChargerActor(chargerActorRef: ActorRef) { }
}
