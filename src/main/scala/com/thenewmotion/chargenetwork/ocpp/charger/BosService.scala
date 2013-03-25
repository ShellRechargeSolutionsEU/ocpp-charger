package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp._
import com.thenewmotion.time.Imports._

/**
 * @author Yaroslav Klymko
 */
trait BosService {
  def fault()
  def available()
  def boot(): Int
  def heartbeat()
  def connector(idx: Int): ConnectorService
}

trait ConnectorService {
  def notification(status: ChargePointStatus)
  def occupied()
  def available()
  def authorize(card: String): Boolean
  def startSession(card: String): Int
  def stopSession(card: Option[String], transactionId: Int): Boolean
}

trait Common {
  protected def service: CentralSystemService

  protected def notification(status: ChargePointStatus, connector: Option[Int] = None) {
    service.statusNotification(connector getOrElse 0, status, Some(DateTime.now))
  }
}


object BosService {
  def apply(chargerId: String, service: CentralSystemService): BosService =
    new BosServiceImpl(chargerId, service)
}

class BosServiceImpl(chargerId: String, protected val service: CentralSystemService) extends BosService with Common {

  def boot(): Int = service.bootNotification(
    chargePointVendor = "The New Motion",
    chargePointModel = "simulator",
    chargePointSerialNumber = Some(chargerId),
    chargeBoxSerialNumber = Some(chargerId),
    firmwareVersion = Some("0.1")
  ).heartbeatInterval

  private val errorCodes = ErrorCodes().iterator

  def fault() {
    notification(Faulted(errorCodes.next(), Some("Random code"), Some("Random code")))
  }

  def available() {
    notification(Available)
  }

  def heartbeat() {
    service.heartbeat
  }

  def connector(idx: Int) = new ConnectorServiceImpl(service, idx)
}


class ConnectorServiceImpl(protected val service: CentralSystemService, c: Int) extends ConnectorService with Common {
  def connectorId = c + 1

  def occupied() {
    notification(Occupied, Some(connectorId))
  }

  def available() {
    notification(Available, Some(connectorId))
  }

  def notification(status: ChargePointStatus) {
    service.statusNotification(connectorId, status)
  }

  def authorize(card: String): Boolean = service.authorize(card).status == AuthorizationAccepted

  def startSession(card: String): Int = service.startTransaction(connectorId, card, DateTime.now, 100)._1

  def stopSession(card: Option[String], transactionId: Int): Boolean =
    service.stopTransaction(transactionId, card, DateTime.now, 200) match {
      case Some(IdTagInfo(AuthorizationAccepted, _, _)) => true
      case _ => false
    }
}