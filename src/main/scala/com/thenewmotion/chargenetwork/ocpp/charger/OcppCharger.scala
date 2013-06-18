package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.CentralSystemClient
import java.net.URI
import com.thenewmotion.ocpp.Version
import dispatch.Http
import akka.actor.Props


class OcppCharger(val chargerId: String, val numConnectors: Int, val ocppVersion: Version.Value, val centralSystemURL: URI, val listenPort: Int) {

  val httpServer = new ChargerHTTPServer(listenPort)

  //FIXME: race condition where remote commands could be received before charger actor added to HTTP server
  val client = CentralSystemClient(chargerId, ocppVersion, centralSystemURL, new Http, Some(httpServer.listenURI))
  val chargerActor = system.actorOf(Props(new ChargerActor(BosService(chargerId, client), numConnectors)))

  httpServer.addChargerActor(chargerActor)
}

