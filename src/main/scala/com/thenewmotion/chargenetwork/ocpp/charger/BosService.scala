package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.chargenetwork.ocpp.charger.ConnectorActor.{Another, Alfen, CpProducer}
import com.thenewmotion.ocpp.messages._
import com.thenewmotion.time.Imports._
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
 * @author Yaroslav Klymko
 */
trait BosService {
  def chargerId: String
  def fault()
  def available()
  def boot(): FiniteDuration
  def heartbeat()
  def connector(idx: Int): ConnectorService
}

trait ConnectorService {
  def occupied()
  def startCharging()
  def available()
  def authorize(card: String): Boolean
  def startSession(card: String, meterValue: Int): Int
  def meterValue(transactionId: Int, meterValue: Int)
  def stopSession(card: Option[String], transactionId: Int, meterValue: Int): Boolean
}

trait Common {
  protected def service: CentralSystem

  protected def notification(status: ChargePointStatus, connector: Option[Int] = None) {
    service(StatusNotificationReq(
      connector.map(ConnectorScope.apply) getOrElse ChargePointScope,
      status,
      Some(DateTime.now),
      None))
  }
}

object BosService {
  def apply(chargerId: String, service: CentralSystem, cpProducer: CpProducer): BosService =
    new BosServiceImpl(chargerId, service, cpProducer)
}

class BosServiceImpl(val chargerId: String, protected val service: CentralSystem, cpProducer: CpProducer) extends BosService with Common {

  def boot(): FiniteDuration = service(BootNotificationReq(
    chargePointVendor = "The New Motion",
    chargePointModel = "simulator",
    chargePointSerialNumber = Some(chargerId),
    chargeBoxSerialNumber = Some(chargerId),
    firmwareVersion = Some("0.1"),
    iccid = None,
    imsi = None, meterType = None,
    meterSerialNumber = None)).heartbeatInterval

  private val errorCodes = ErrorCodes().iterator

  def fault() {
    notification(Faulted(Some(errorCodes.next()), Some("Random code"), Some("Random code")))
  }

  def available() {
    notification(Available())
  }

  def heartbeat() {
    service.heartbeat
  }

  def connector(idx: Int) = new ConnectorServiceImpl(service, idx, cpProducer)
}

class ConnectorServiceImpl(protected val service: CentralSystem, connectorId: Int, cpProducer: CpProducer) extends ConnectorService with Common {

  private val random = new Random()

  /**
   * OCPP 1.5 doesn't distinguish between charging and not charging
   * while connected.
   * At least one vendor of EV chargers communicates the difference
   * using a separate field "info" which is present and contains the
   * value "charging" if the car is still charging.
   *
   * This case is covered by this method.
   */
  def startCharging() {
    val occupied = cpProducer match {
      case Alfen => Occupied(Some("Charging"))
      case Another => Occupied(None)
    }
    notification(occupied, Some(connectorId))
  }

  def occupied() {
    notification(Occupied(None), Some(connectorId))
  }

  def available() {
    notification(Available(), Some(connectorId))
  }

  def authorize(card: String) = service(AuthorizeReq(card)).idTag.status == AuthorizationStatus.Accepted

  def startSession(card: String, meterValue: Int) =
    service(StartTransactionReq(ConnectorScope(connectorId), card, DateTime.now, meterValue, None)).transactionId

  def meterValue(transactionId: Int, meterValue: Int) {
    val meter = Meter(DateTime.now, List(Meter.DefaultValue(meterValue)))
    service(MeterValuesReq(ConnectorScope(connectorId), Some(transactionId), List(meter)))
  }

  def stopSession(card: Option[String], transactionId: Int, meterValue: Int): Boolean =
    service(StopTransactionReq(transactionId, card, DateTime.now, meterValue, Nil))
      .idTag.exists(_.status == AuthorizationStatus.Accepted)
}