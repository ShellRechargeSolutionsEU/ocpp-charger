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
  def occupied()
  def available()
  def authorize(card: String): Boolean
  def startSession(card: String, meterValue: Int): Int
  def meterValue(transactionId: Int, meterValue: Int)
  def stopSession(card: Option[String], transactionId: Int, meterValue: Int): Boolean
}

trait Common {
  protected def service: CentralSystemService

  protected def notification(status: ChargePointStatus, connector: Option[Int] = None) {
    service.statusNotification(
      connector.map(ConnectorScope.apply) getOrElse ChargePointScope,
      status,
      Some(DateTime.now),
      None)
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
    firmwareVersion = Some("0.1"),
    iccid = None,
    imsi = None, meterType = None,
    meterSerialNumber = None).heartbeatInterval.toSeconds.toInt

  private val errorCodes = ErrorCodes().iterator

  def fault() {
    notification(Faulted(Some(errorCodes.next()), Some("Random code"), Some("Random code")))
  }

  def available() {
    notification(Available)
  }

  def heartbeat() {
    service.heartbeat
  }

  def connector(idx: Int) = new ConnectorServiceImpl(service, idx)
}

class ConnectorServiceImpl(protected val service: CentralSystemService, connectorId: Int) extends ConnectorService with Common {

  def occupied() {
    notification(Occupied, Some(connectorId))
  }

  def available() {
    notification(Available, Some(connectorId))
  }

  def authorize(card: String) = service.authorize(card).status == AuthorizationStatus.Accepted

  def startSession(card: String, meterValue: Int) =
    service.startTransaction(ConnectorScope(connectorId), card, DateTime.now, meterValue, None)._1

  def meterValue(transactionId: Int, meterValue: Int) {
    val meter = Meter(DateTime.now, List(Meter.DefaultValue(meterValue)))
    service.meterValues(ConnectorScope(connectorId), Some(transactionId), List(meter))
  }

  def stopSession(card: Option[String], transactionId: Int, meterValue: Int): Boolean =
    service.stopTransaction(transactionId, card, DateTime.now, meterValue, Nil) match {
      case Some(IdTagInfo(AuthorizationStatus.Accepted, _, _)) => true
      case _ => false
    }
}