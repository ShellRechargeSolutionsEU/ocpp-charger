package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Actor, ActorRef}
import scala.concurrent.duration._
import scala.language.postfixOps

class UserActor(charger: ActorRef, c: Int, actions: ActionIterator) extends Actor {
  import com.thenewmotion.chargenetwork.ocpp.charger.{ActionIterator => AT}
  import ChargerActor._
  import context.dispatcher

  case object Act

  override def preStart() {
    context.system.scheduler.schedule(3 seconds, 2 seconds, self, Act)
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