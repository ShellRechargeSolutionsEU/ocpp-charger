package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Props, ActorSystem}
import com.thenewmotion.ocpp.{CentralSystemClientV12, CentralSystemClientV15}
import java.net.URI
import dispatch._
import com.thenewmotion.common.dispatch.Slf4jLogger

/**
 * @author Yaroslav Klymko
 */
object OcppCharger extends App {
  implicit val system = ActorSystem("ocpp-simulator")
  val uri  = new URI("http://127.0.0.1:8081/ocpp/")

//  val client = new CentralSystemClientV15("00055103978E", uri, new Http with thread.Safety with Slf4jLogger)
  val client = new CentralSystemClientV12("00055103978E", uri, new Http with thread.Safety with Slf4jLogger)

  val numberOfConnectors = 2
  val charger = system.actorOf(Props(new ChargerActor(BosService("00055103978E", client), numberOfConnectors)))

  (0 until numberOfConnectors) map {
    c => system.actorOf(Props(new UserActor(charger, c, ActionIterator())))
  }
}