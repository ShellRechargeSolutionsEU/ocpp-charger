package com.thenewmotion.chargenetwork
package ocpp.charger

import akka.actor.{Actor, Props, ActorRef}
import akka.io.IO
import akka.io.Tcp.Connected
import com.thenewmotion.ocpp._
import com.thenewmotion.ocpp.spray.{OcppProcessing, ChargerInfo}
import com.typesafe.scalalogging.slf4j.Logging
import java.net.URI
import _root_.spray.can.Http.Register
import _root_.spray.can.Http
import _root_.spray.http.{HttpResponse, HttpRequest}


class ChargerHTTPServer(serviceFunction: ChargerInfo => Option[ChargePointService], listenPort: Int) extends Logging {
  private val interface = "localhost"

  private val requestHandler: ActorRef = system.actorOf(Props(new ChargerServerActor))
  IO(Http) ! Http.Bind(requestHandler, interface = interface, port = listenPort)

  private class ChargerServerActor extends Actor {
    def receive = {
      case Connected(_, _) =>
        sender ! Register(self)

      case req: HttpRequest =>
        logger.debug(s"Received HTTP request: ${req}")
        sender ! handleRequest(req)
    }

    def handleRequest(req: HttpRequest): HttpResponse = {
      val result = OcppProcessing[ChargePointService](req, serviceFunction)
      result match {
        case Left(error) => error
        case Right((chargerId, msg)) => msg()
      }
    }
  }

  def listenURI: URI = new URI("http", null, interface, listenPort, null, null, null)
}
