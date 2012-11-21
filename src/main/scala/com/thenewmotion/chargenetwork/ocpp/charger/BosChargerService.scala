package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.chargenetwork.{ocpp => xb}
import scalaxb.Fault

/**
 * @author Yaroslav Klymko
 */

trait BosService {
  def service: xb.CentralSystemService
  def chargerId: String

  def notification(status: Either[xb.ChargePointErrorCode, xb.ChargePointStatus], connector: Option[Int]) {
    def inner(s: xb.ChargePointStatus, e: xb.ChargePointErrorCode) =
      service.statusNotification(xb.StatusNotificationRequest(connector getOrElse 0, s, e), chargerId)

    status.fold(inner(xb.Faulted, _), inner(_, xb.NoError))
  }

  def error(fault: Fault[_]) = sys.error(fault.original.toString)
}

trait BosChargerService {
  def boot(): Int
  def notification(status: xb.ChargePointStatus)
  def fault(errorCode: xb.ChargePointErrorCode)
  def heartbeat()
  def connectorService(idx: Connector): BosConnectorService
}

trait BosConnectorService {
  def notification(status: xb.ChargePointStatus)
  def authorize(card: Card): Boolean
  def startSession(card: Card): Int
  def stopSession(card: Option[Card], transactionId: Int): Boolean
}

object BosChargerService {
  def apply(chargerId: String, service: xb.CentralSystemService): BosChargerService =
    new BosChargerServiceImpl(chargerId, service)
}

class BosChargerServiceImpl(val chargerId: String,
                            val service: xb.CentralSystemService)
  extends BosChargerService with BosService {

  def boot(): Int =
    service.bootNotification(
      xb.BootNotificationRequest(
        "The New Motion",
        "simulator",
        Some(chargerId),
        Some(chargerId)
      ), chargerId)
      .fold(error, _.heartbeatInterval getOrElse 15)


  def notification(status: xb.ChargePointStatus) {
    notification(Right(status), None)
  }

  def fault(errorCode: xb.ChargePointErrorCode) {
    notification(Left(errorCode), None)
  }

  def heartbeat() {
    service.heartbeat(xb.HeartbeatRequest(), chargerId)
  }

  def connectorService(idx: Connector) = new BosConnectorServiceImpl(service, chargerId, idx)
}

class BosConnectorServiceImpl(val service: xb.CentralSystemService,
                              val chargerId: String,
                              c: Connector) extends BosConnectorService with BosService {
  import com.thenewmotion.time.Imports._

  def connectorId = c.id + 1

  def notification(status: xb.ChargePointStatus) {
    notification(Right(status), Some(connectorId))
  }

  def authorize(card: Card): Boolean =
    service.authorize(xb.AuthorizeRequest(card.id), chargerId)
    .fold(error, _.status == xb.AcceptedValue7)


  def startSession(card: Card): Int =
    service.startTransaction(
      xb.StartTransactionRequest(connectorId, card.id, DateTime.now, 0),
      chargerId)
      .fold(error, _.transactionId)

  def stopSession(card: Option[Card], transactionId: Int): Boolean = {
    service.stopTransaction(
      xb.StopTransactionRequest(transactionId, card.map(_.id), DateTime.now, 100),
      chargerId) match {
      case Right(xb.StopTransactionResponse(Some(xb.IdTagInfo(xb.AcceptedValue7, _, _)))) => true
      case _ => false
    }
  }
}