package com.thenewmotion.chargenetwork
package ocpp.charger

import spray.routing.SimpleRoutingApp
import java.net.URI
import akka.actor.ActorRef
import scala.xml.{XML, NodeSeq}
import spray.httpx.unmarshalling._
import spray.http.{HttpBody, MediaTypes}
import java.io.{ByteArrayInputStream, InputStreamReader}


class ChargerHTTPServer(val listenPort: Int) extends SimpleRoutingApp {

  private val interface = "localhost"

  startServer(interface = interface, port = listenPort)(route)

  private def route = dynamic {
    post {
      entity(soap12Unmarshaller) { (e) =>
        System.err.println("Received remote command: " + e)
        complete("We received request, but will remain passive until we have been enhanced further")
      }
    }
  }

  implicit val soap12Unmarshaller = Unmarshaller.forNonEmpty {
    Unmarshaller[NodeSeq](MediaTypes.`application/soap+xml`) {
      case HttpBody(contentType, buffer) =>
        XML.load(new InputStreamReader(new ByteArrayInputStream(buffer), contentType.charset.nioCharset))
    }
  }

  def listenURI: URI = new URI("http", null, interface, listenPort, null, null, null)

  def addChargerActor(chargerActorRef: ActorRef) { }
}
