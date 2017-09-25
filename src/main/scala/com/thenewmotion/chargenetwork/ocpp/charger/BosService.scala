package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.messages._
import java.time._
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

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
      Some(ZonedDateTime.now),
      None))
  }
}

object BosService {
  def apply(chargerId: String, service: CentralSystem): BosService =
    new BosServiceImpl(chargerId, service)
}

class BosServiceImpl(val chargerId: String, protected val service: CentralSystem) extends BosService with Common {

  def boot(): FiniteDuration = service(BootNotificationReq(
    chargePointVendor = "The New Motion",
    chargePointModel = "simulator",
    chargePointSerialNumber = Some(chargerId),
    chargeBoxSerialNumber = Some(chargerId),
    firmwareVersion = Some("0.1"),
    iccid = None,
    imsi = None, meterType = None,
    meterSerialNumber = None)).interval

  private val errorCodes = ErrorCodes().iterator

  def fault() {
    notification(ChargePointStatus.Faulted(
      Some(errorCodes.next()),
      Some("Random code"),
      Some("Random code")
    ))
  }

  def available() {
    notification(ChargePointStatus.Available())
  }

  def heartbeat() {
    service.heartbeat
  }

  def connector(idx: Int) = new ConnectorServiceImpl(service, idx)
}

class ConnectorServiceImpl(protected val service: CentralSystem, connectorId: Int) extends ConnectorService with Common {

  private val random = new Random()

  def occupied() {
    random.nextBoolean() match {
      case true => notification(
        ChargePointStatus.Occupied(
          Some(OccupancyKind.Charging)
        ),
        Some(connectorId)
      )
      case false =>
        notification(ChargePointStatus.Occupied(None), Some(connectorId))
    }

  }

  def available() {
    notification(ChargePointStatus.Available(), Some(connectorId))
  }

  def authorize(card: String) = service(AuthorizeReq(card)).idTag.status == AuthorizationStatus.Accepted

  def startSession(card: String, meterValue: Int) =
    service(StartTransactionReq(
      ConnectorScope(connectorId),
      card,
      ZonedDateTime.now,
      meterValue,
      None)
    ).transactionId

  def meterValue(transactionId: Int, meterValue: Int) {
    val m = meter.Meter(
      ZonedDateTime.now,
      List(meter.DefaultValue(meterValue))
    )
    service(MeterValuesReq(ConnectorScope(connectorId), Some(transactionId), List(m)))
  }

  def stopSession(card: Option[String], transactionId: Int, meterValue: Int): Boolean =
    service(StopTransactionReq(
      transactionId,
      card,
      ZonedDateTime.now,
      meterValue,
      meters = Nil,
      reason = StopReason.Local
    )).idTag.exists(_.status == AuthorizationStatus.Accepted)
}