package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.{ChargePointService, CentralSystemClient, Version}
import java.net.URI
import dispatch.Http
import akka.actor.Props
import com.thenewmotion.ocpp.spray.ChargerInfo


class OcppCharger(chargerId: String,
                  numConnectors: Int,
                  ocppVersion: Version.Value,
                  centralSystemURL: URI,
                  listenPort: Int) {
  val httpServer = new ChargerHTTPServer(serviceForCharger, listenPort)

  val client = CentralSystemClient(chargerId, ocppVersion, centralSystemURL, new Http, Some(httpServer.listenURI))
  val chargerActor = system.actorOf(Props(new ChargerActor(BosService(chargerId, client), numConnectors)))

  private[charger] def serviceForCharger(chargerInfo: ChargerInfo): Option[ChargePointService] =
    if (chargerInfo.chargerId == chargerId) Some(LoggingChargePointService)
    else None
}

