package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor._
import akka.util.duration._

/**
 * @author Yaroslav Klymko
 */
class ChargerActor(service: BosService, numberOfConnectors: Int = 1)
  extends Actor
  with LoggingFSM[ChargerActor.State, ChargerActor.Data] {

  import ChargerActor._

  override def preStart() {
    val interval = service.boot() / 100
    service.available()
    context.system.scheduler.schedule(1 second, interval seconds, self, Heartbeat)
    scheduleFault()

    (0 until numberOfConnectors).map(startConnector)
  }

  def scheduleFault() {
    context.system.scheduler.scheduleOnce(30 seconds, self, Fault)
  }

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
      service.fault()
      goto(Faulted) forMax 5.seconds
  }

  when(Faulted) {
    case Event(StateTimeout, _) =>
      service.available()
      scheduleFault()
      goto(Available)
    case Event(_: UserAction, _) => stay()
  }

  initialize

  def startConnector(c: Int) {
    context.actorOf(Props(new ConnectorActor(service.connector(c))), c.toString)
  }

  def connector(c: Int): ActorRef = context.actorFor(c.toString)

  def dispatch(msg: ConnectorActor.Action, c: Int){
    connector(c) ! msg
  }
}

object ChargerActor {
  sealed trait State
  case object Available extends State
  case object Faulted extends State

  sealed trait Data
  val NoData = PluggedConnectors(Set())
  case class PluggedConnectors(ids: Set[Int]) extends Data

  sealed trait Action
  case object Heartbeat extends Action
  case object Fault extends Action

  sealed trait UserAction extends Action
  case class Plug(connector: Int) extends UserAction
  case class Unplug(connector: Int) extends UserAction
  case class SwipeCard(connector: Int, card: String) extends UserAction
}
