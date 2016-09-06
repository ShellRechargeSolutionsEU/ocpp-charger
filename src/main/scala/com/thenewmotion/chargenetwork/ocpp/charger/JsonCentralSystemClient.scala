package com.thenewmotion.chargenetwork.ocpp.charger

import com.typesafe.scalalogging.slf4j.LazyLogging
import java.net.URI
import com.thenewmotion.ocpp.messages._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.thenewmotion.ocpp.json._

@deprecated("Use OcppJsonClient directly instead", since="2.0")
class JsonCentralSystemClient(chargerId: String, centralSystemUri: URI, messageEncryption: Boolean) extends CentralSystem with LazyLogging {

  val client = new OcppJsonClient(chargerId, centralSystemUri, messageEncryption) {
    def onError(err: OcppError) = logger.error(s"Received OCPP error $err")

    def onRequest(req: ChargePointReq) = {
      logger.warn("Received OCPP request {} but request handling not implemented for OCPP-J", req)
      Future { throw new OcppException(OcppError(PayloadErrorCode.NotImplemented, "not implemented")) }
    }

    def onDisconnect = logger.error("WebSocket disconnected for charger {}" , chargerId)
  }

  def syncSend[REQ <: CentralSystemReq, RES <: CentralSystemRes](req: REQ)
                                                                (implicit reqRes: ReqRes[REQ, RES]): RES = {
    val res = Await.result(client.send(req), 45.seconds)
    logger.info("{}\n\t>> {}\n\t<< {}", chargerId, req, res)
    res
  }

  def authorize(req: AuthorizeReq): AuthorizeRes = syncSend[AuthorizeReq, AuthorizeRes](req)

  def bootNotification(req: BootNotificationReq): BootNotificationRes = syncSend(req)

  def dataTransfer(req: CentralSystemDataTransferReq): CentralSystemDataTransferRes = syncSend(req)

  def diagnosticsStatusNotification(req: DiagnosticsStatusNotificationReq) = syncSend(req)

  def firmwareStatusNotification(req: FirmwareStatusNotificationReq) = syncSend(req)

  def heartbeat: HeartbeatRes = syncSend(HeartbeatReq)

  def meterValues(req: MeterValuesReq) = syncSend(req)

  def startTransaction(req: StartTransactionReq): StartTransactionRes = syncSend(req)

  def statusNotification(req: StatusNotificationReq) = syncSend(req)

  def stopTransaction(req: StopTransactionReq): StopTransactionRes = syncSend(req)
}
