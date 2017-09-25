package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp.messages.ChargePointErrorCode

object ErrorCodes {

  import ChargePointErrorCode._

  private val codes = List(
    ConnectorLockFailure,
    HighTemperature,
    EVCommunicationError,
    PowerMeterFailure,
    PowerSwitchFailure,
    ReaderFailure,
    ResetFailure)

  def apply(): Stream[ChargePointErrorCode] = codes.toStream #::: apply()
}