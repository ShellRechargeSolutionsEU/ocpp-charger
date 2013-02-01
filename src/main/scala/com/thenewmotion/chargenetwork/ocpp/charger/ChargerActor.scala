package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor._
import com.thenewmotion.chargenetwork.{ocpp => xb}
import concurrent.duration._

/**
 * @author Yaroslav Klymko
 */
class ChargerActor(service: BosChargerService, numberOfConnectors: Int = 1)
  extends Actor
  with LoggingFSM[ChargerActor.State, ChargerActor.Data] {

  import ChargerActor._
  import context.dispatcher

  override def preStart() {
    val interval = service.boot()
    service.notification(xb.Available)

    context.system.scheduler.schedule(1 second, interval seconds, self, Heartbeat)
    scheduleFault()

    (0 until numberOfConnectors).map(x => startConnector(x))
  }

  def scheduleFault() {
    context.system.scheduler.scheduleOnce(30 seconds, self, Fault)
  }

  val errorCodes = ErrorCodes().iterator

  startWith(Available, NoData)

  when(Available) {
    case Event(Heartbeat, _) =>
      service.heartbeat()
      stay()
    case Event(Plug(c), PluggedConnectors(cs)) =>
      if (!cs.contains(c)) dispatch(ConnectorActor.Plug, c)
      stay() using PluggedConnectors(cs + c)
    case Event(Unplug(c), PluggedConnectors(cs)) =>
      if (cs.contains(c)) dispatch(ConnectorActor.Unplug, c)
      stay() using PluggedConnectors(cs - c)
    case Event(SwipeCard(c, card), PluggedConnectors(cs)) =>
      if (cs.contains(c)) dispatch(ConnectorActor.SwipeCard(card), c)
      stay()
    case Event(Fault, _) =>
      service.fault(errorCodes.next())
      context.system.scheduler.scheduleOnce(5 seconds, self, StateTimeout)
      goto(Faulted)
  }

  when(Faulted) {
    case Event(StateTimeout, _) =>
      service.notification(xb.Available)
      scheduleFault()
      goto(Available)
    case Event(_: UserAction, _) => stay()
  }

  initialize

  def startConnector(c: Int) {
    context.actorOf(Props(new ConnectorActor(service.connectorService(c))), c.toString)
  }

  def connector(c: Int): ActorRef = context.actorFor(c.toString)

  def dispatch(msg: ConnectorActor.Action, c: Connector){
    connector(c.id) ! msg
  }
}

object ChargerActor {
  sealed trait State
  case object Available extends State
  case object Faulted extends State

  sealed trait Data
  val NoData = PluggedConnectors(Set())
  case class PluggedConnectors(ids: Set[Connector]) extends Data

  sealed trait Action
  case object Heartbeat extends Action
  case object Fault extends Action

  sealed trait UserAction extends Action
  case class Plug(connector: Connector) extends UserAction
  case class Unplug(connector: Connector) extends UserAction
  case class SwipeCard(connector: Connector, card: Card) extends UserAction
}
