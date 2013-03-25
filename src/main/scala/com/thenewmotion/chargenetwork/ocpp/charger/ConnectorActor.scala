package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor._

/**
 * @author Yaroslav Klymko
 */
class ConnectorActor(service: ConnectorService)
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
      if (service.authorize(rfid)) goto(Charging) using ChargingData(service.startSession(rfid))
      else stay()
    case Event(Unplug, _) =>
      service.available()
      goto(Available)
  }

  when(Charging) {
    case Event(SwipeCard(rfid), ChargingData(transactionId)) =>
      if (service.authorize(rfid) && service.stopSession(Some(rfid), transactionId))
        goto(Connected) using (NoData)
      else stay()
    case Event(_: Action, _) => stay()
  }

  onTermination {
    case StopEvent(_, Charging, ChargingData(transactionId)) =>
      service.stopSession(None, transactionId)
  }
}

object ConnectorActor {
  sealed trait State
  case object Available extends State
  case object Connected extends State
  case object Charging extends State

  sealed trait Action
  case object Plug extends Action
  case object Unplug extends Action
  case class SwipeCard(rfid: String) extends Action
  case object Fault

  sealed abstract class Data
  case object NoData extends Data
  case class ChargingData(transactionId: Int) extends Data
}
