package com.thenewmotion.chargenetwork.ocpp

/**
 * @author Yaroslav Klymko
 */
package object charger {
  implicit class Card(val id: String) extends AnyVal {
    override def toString = id
  }

  implicit class Connector(val id: Int) extends AnyVal {
    override def toString = id.toString
  }
}