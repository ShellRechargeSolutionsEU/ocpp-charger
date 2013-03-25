package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Actor, ActorRef}
import akka.util.duration._

/**
 * @author Yaroslav Klymko
 */
class UserActor(charger: ActorRef, c: Int, actions: ActionIterator) extends Actor {
  import com.thenewmotion.chargenetwork.ocpp.charger.{ActionIterator => AT}
  import ChargerActor._

  case object Act

  override def preStart() {
    context.system.scheduler.schedule(1 second, 1 second, self, Act)
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