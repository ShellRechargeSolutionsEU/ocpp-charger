package com.thenewmotion.chargenetwork.ocpp.charger
package json

import com.thenewmotion.ocpp.messages.centralsystem.CentralSystem
import com.typesafe.scalalogging.slf4j.Logging
import java.net.URI
import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json.PayloadErrorCode
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@deprecated("Use OcppJsonClient directly instead")
class JsonCentralSystemClient(chargerId: String, centralSystemUri: URI) extends CentralSystem with Logging {

  val client = new OcppJsonClient(chargerId, centralSystemUri) {
    def onError(err: OcppError) = logger.error(s"Received OCPP error $err")

    def onRequest(req: ChargePointReq) = {
      logger.warn("Received OCPP request {} but request handling not implemented for OCPP-J", req)
      Future { throw new OcppException(OcppError(PayloadErrorCode.NotImplemented, "not implemented")) }
    }

    def onDisconnect = logger.error("WebSocket disconnected for charger {}" , chargerId)
  }

  def syncSend(req: CentralSystemReq) = Await.result(client.send(req), 45.seconds)

  def authorize(req: AuthorizeReq): AuthorizeRes = syncSend(req).asInstanceOf[AuthorizeRes]

  def bootNotification(req: BootNotificationReq): BootNotificationRes = syncSend(req).asInstanceOf[BootNotificationRes]

  def dataTransfer(req: DataTransferReq): DataTransferRes = syncSend(req).asInstanceOf[DataTransferRes]

  def diagnosticsStatusNotification(req: DiagnosticsStatusNotificationReq) = syncSend(req)

  def firmwareStatusNotification(req: FirmwareStatusNotificationReq) = syncSend(req)

  def heartbeat: HeartbeatRes = syncSend(HeartbeatReq).asInstanceOf[HeartbeatRes]

  def meterValues(req: MeterValuesReq) = syncSend(req)

  def startTransaction(req: StartTransactionReq): StartTransactionRes = syncSend(req).asInstanceOf[StartTransactionRes]

  def statusNotification(req: StatusNotificationReq) = syncSend(req)

  def stopTransaction(req: StopTransactionReq): StopTransactionRes = syncSend(req).asInstanceOf[StopTransactionRes]
}
