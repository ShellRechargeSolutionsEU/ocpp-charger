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
  def random(card: String): ActionIterator = new RandomIterator(card)

  def apply(card: String): ActionIterator = random(card)

  sealed trait UserAction
  case object Plug extends UserAction
  case object Unplug extends UserAction
  case class SwipeCard(card: String) extends UserAction
}

class RandomIterator(card: String) extends ActionIterator {
  private val Random = new Random()


  def next(): UserAction = Random.nextInt(4) match {
    case 0 => Plug
    case 1 => Unplug
    case _ => SwipeCard(card)
  }
}