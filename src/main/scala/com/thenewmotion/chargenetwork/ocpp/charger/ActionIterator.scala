package com.thenewmotion.chargenetwork.ocpp.charger

import util.Random
import ActionIterator._

/**
 * @author Yaroslav Klymko
 */

trait ActionIterator {
  def next(): UserAction
}

object ActionIterator {
  def random(): ActionIterator = new RandomIterator

  def apply(): ActionIterator = random()

  sealed trait UserAction
  case object Plug extends UserAction
  case object Unplug extends UserAction
  case class SwipeCard(card: Card) extends UserAction
}

class RandomIterator extends ActionIterator {
  private val Random = new Random()


  def next(): UserAction = Random.nextInt(4) match {
    case 0 => Plug
    case 1 => Unplug
    case 2 | 3 => SwipeCard(Card("3E60A5E2"))
  }
}