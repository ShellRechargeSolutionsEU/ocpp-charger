package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Props, ActorSystem}
import com.thenewmotion.ocpp.{Version, CentralSystemClient}
import java.net.URI
import dispatch._

/**
 * @author Yaroslav Klymko
 */
object OcppCharger extends App {
  implicit val system = ActorSystem("ocpp-simulator")
  val uri = new URI("http://127.0.0.1:8081/ocpp/")

  val client = CentralSystemClient("00055103978E", Version.V15, uri, new Http)

  val numberOfConnectors = 2
  val charger = system.actorOf(Props(new ChargerActor(BosService("00055103978E", client), numberOfConnectors)))

  (0 until numberOfConnectors) map {
    c => system.actorOf(Props(new UserActor(charger, c, ActionIterator())))
  }
}