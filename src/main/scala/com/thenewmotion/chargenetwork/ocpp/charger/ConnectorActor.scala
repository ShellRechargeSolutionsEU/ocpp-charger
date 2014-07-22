package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor._
import scala.concurrent.duration._

/**
 * @author Yaroslav Klymko
 */
class ConnectorActor(service: ConnectorService, alfenCharger:Boolean = false)
  extends Actor
  with LoggingFSM[ConnectorActor.State, ConnectorActor.Data] {
  import ConnectorActor._

  startWith(Available, NoData)

  when(Available) {
    case Event(Plug, _) =>
      service.occupied()
      goto(Connected)
  }

  when(Connected) {
    case Event(SwipeCard(rfid), _)  =>
      if (service.authorize(rfid)) {
        val sessionId = service.startSession(rfid, initialMeterValue)
        if (alfenCharger) service.occupiedCharging() else service.occupied()
        log.debug("Going to 'Charging'")
        goto(Charging) using ChargingData(sessionId, initialMeterValue)
      }
      else stay()
    case Event(Unplug, _) =>
      service.available()
      goto(Available)
  }

  when(Charging) {
    case Event(SwipeCard(rfid), ChargingData(transactionId, meterValue)) =>
      log.debug("Card swiped while charging")
      if (service.authorize(rfid) && service.stopSession(Some(rfid), transactionId, meterValue))
        goto(Connected) using NoData
      else stay()
    case Event(SendMeterValue, ChargingData(transactionId, meterValue)) => {
      log.debug("Sending meter value")
      service.meterValue(transactionId, meterValue)
      stay() using ChargingData(transactionId, meterValue + 1)
    }
    case Event(FullyCharged, _) =>
      log.debug("FullyCharged; sending 'occupied'")
      service.occupied()
      goto(Connected) using NoData
    case Event(_: Action, _) => stay()
  }

  onTransition {
    case _ -> Charging => {
      log.debug("Setting timer for meterValue and fullyCharged")
      setTimer("fullyCharged", FullyCharged, 1 seconds, false)
      setTimer("meterValueTimer", SendMeterValue, 20 seconds, true)
    }
    case Charging -> _ => cancelTimer("meterValueTimer")
  }

  onTermination {
    case StopEvent(_, Charging, ChargingData(transactionId, meterValue)) =>
      service.stopSession(None, transactionId, meterValue)
  }
}

object ConnectorActor {
  val initialMeterValue = 100

  sealed trait State
  case object Available extends State
  case object Connected extends State
  case object Charging extends State

  sealed trait Action
  case object Plug extends Action
  case object Unplug extends Action
  case object FullyCharged extends Action
  case class SwipeCard(rfid: String) extends Action
  case object Fault

  sealed abstract class Data
  case object NoData extends Data
  case class ChargingData(transactionId: Int, meterValue: Int) extends Data

  case object SendMeterValue
}
