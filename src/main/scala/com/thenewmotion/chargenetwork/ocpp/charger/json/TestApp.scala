package com.thenewmotion.chargenetwork.ocpp.charger.json

import java.net.URI
import com.typesafe.scalalogging.slf4j.Logging
import com.thenewmotion.ocpp.messages._
import scala.concurrent._
import com.thenewmotion.ocpp.messages.GetConfigurationReq
import scala.concurrent.ExecutionContext.Implicits.global

object TestApp extends App {
  val connection = new OcppJsonClient("REFACHA", new URI("http://localhost:8080/ocppws")) with Logging {

    def onRequest(req: ChargePointReq): Future[ChargePointRes] = req match {
      case GetConfigurationReq(_) => Future { throw new Exception("Blaargh! No config!") }
      case GetLocalListVersionReq => Future { GetLocalListVersionRes(AuthListNotSupported) }
    }

    def onError(err: OcppError) = logger.warn(s"OCPP error: ${err.error} ${err.description}")

    def onDisconnect = logger.warn("WebSocket disconnect")
  }

  connection.send(BootNotificationReq("TNM", "Lolo 3", Some("03000001"), None, None, None, None, None, None))

  Thread.sleep(7000)

  connection.close()
}
