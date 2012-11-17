package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Actor, ActorRef}
import concurrent.duration._

/**
 * @author Yaroslav Klymko
 */
class UserActor(charger: ActorRef, c: Connector, actions: ActionIterator) extends Actor {
  import com.thenewmotion.chargenetwork.ocpp.charger.{ActionIterator => AT}
  import ChargerActor._
  import context.dispatcher

  case object Act

  override def preStart() {
    context.system.scheduler.schedule(1 second, 2 seconds, self, Act)
  }

  def receive = {
    case Act =>
      val action = actions.next() match {
        case AT.Plug => Plug(c)
        case AT.Unplug => Unplug(c)
        case AT.SwipeCard(card) => SwipeCard(c, card)
      }
      charger ! action
  }
}