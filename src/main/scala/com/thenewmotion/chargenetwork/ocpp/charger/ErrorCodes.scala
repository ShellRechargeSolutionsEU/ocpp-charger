package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.centralsystem.ChargePointErrorCode

/**
 * @author Yaroslav Klymko
 */
object ErrorCodes {

  import ChargePointErrorCode._

  private val codes = List(
    ConnectorLockFailure,
    HighTemperature,
    Mode3Error,
    PowerMeterFailure,
    PowerSwitchFailure,
    ReaderFailure,
    ResetFailure)

  def apply(): Stream[ChargePointErrorCode.Value] = codes.toStream #::: apply()
}