package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor._
import com.thenewmotion.chargenetwork.{ocpp => xb}

/**
 * @author Yaroslav Klymko
 */
class ConnectorActor(bos: BosConnectorService)
  extends Actor
  with LoggingFSM[ConnectorActor.State, ConnectorActor.Data] {
  import ConnectorActor._

  startWith(Available, NoData)

  when(Available) {
    case Event(Plug, _) =>
      bos.notification(xb.Occupied)
      goto(Connected)
  }

  when(Connected) {
    case Event(SwipeCard(rfid), _)  =>
      if (bos.authorize(rfid)) goto(Charging) using ChargingData(bos.startSession(rfid))
      else stay()
    case Event(Unplug, _) =>
      bos.notification(xb.Available)
      goto(Available)
  }

  when(Charging) {
    case Event(SwipeCard(rfid), ChargingData(transactionId)) =>
      if (bos.authorize(rfid) && bos.stopSession(Some(rfid), transactionId))
        goto(Connected) using (NoData)
      else stay()
    case Event(_: Action, _) => stay()
  }

  onTermination {
    case StopEvent(_, Charging, ChargingData(transactionId)) =>
      bos.stopSession(None, transactionId)
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
  case class SwipeCard(rfid: Card) extends Action
  case object Fault

  sealed abstract class Data
  case object NoData extends Data
  case class ChargingData(transactionId: Int) extends Data
}
