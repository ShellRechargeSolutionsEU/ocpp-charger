package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.{ChargePointService, CentralSystemClient, Version}
import java.net.URI
import dispatch.Http
import akka.actor.Props
import com.thenewmotion.ocpp.spray.ChargerInfo


class OcppCharger(val chargerId: String, val numConnectors: Int, val ocppVersion: Version.Value,
                  val centralSystemURL: URI, val listenPort: Int) {
  val httpServer = new ChargerHTTPServer(serviceForCharger, listenPort)

  //FIXME: race condition where remote commands could be received before charger actor added to HTTP server
  val client = CentralSystemClient(chargerId, ocppVersion, centralSystemURL, new Http, Some(httpServer.listenURI))
  val chargerActor = system.actorOf(Props(new ChargerActor(BosService(chargerId, client), numConnectors)))

  httpServer.addChargerActor(chargerActor)

  private[charger] def serviceForCharger(chargerInfo: ChargerInfo): Option[ChargePointService] =
    if (chargerInfo.chargerId == chargerId)
      Some(LoggingChargePointService)
    else
      None
}

