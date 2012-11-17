package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.chargenetwork.ocpp._

/**
 * @author Yaroslav Klymko
 */
object ErrorCodes {
  private val codes = List(
    ConnectorLockFailure,
    HighTemperature,
    Mode3Error,
    PowerMeterFailure,
    PowerSwitchFailure,
    ReaderFailure,
    ResetFailure)

  def apply(): Stream[ChargePointErrorCode] = codes.toStream #::: apply()
}
