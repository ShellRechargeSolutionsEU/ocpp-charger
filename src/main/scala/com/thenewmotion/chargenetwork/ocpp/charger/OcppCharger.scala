package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Props, ActorSystem}

import com.thenewmotion.chargenetwork.ocpp.CentralSystemServiceSoapBindings
import scalaxb.{SoapClients, DispatchHttpClients}
import concurrent.duration._

/**
 * @author Yaroslav Klymko
 */
object OcppCharger extends App {
  implicit val system = ActorSystem("ocpp-simulator")

  val bindings = new CentralSystemServiceSoapBindings with SoapClients with DispatchHttpClients {
    override def baseAddress = new java.net.URI("http://127.0.0.1:8081/ocpp/")
  }

  val numberOfConnectors = 2

  val charger = system.actorOf(Props(new ChargerActor(BosChargerService(bindings.service), numberOfConnectors)))

  (0 until numberOfConnectors) map {
    c => system.actorOf(Props(new UserActor(charger, c, ActionIterator())))
  }
}