package com.thenewmotion.chargenetwork.ocpp.charger

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.mock.Mockito
import akka.testkit.{TestFSMRef, TestKit}
import akka.actor.{FSM, ActorSystem}
import ChargerActor._

/**
 * @author Yaroslav Klymko
 */
class ChargerActorSpec extends SpecificationWithJUnit with Mockito {

  "ChargerActor" should {

    "start connectors and notify on boot" in new ChargerActorScope {
      actor.stateName mustEqual Available
      actor.stateData mustEqual NoData
      there was one(service).boot()
    }

    "send heartbeat" in new ChargerActorScope {
      actor receive Heartbeat
      there was one(service).heartbeat()
    }

    "become faulted" in new ChargerActorScope {
      actor receive Fault
      actor.stateName mustEqual Faulted
      actor receive FSM.StateTimeout
      actor.stateName mustEqual Available
    }

    "dispatch messages to connectors" in new ChargerActorScope {
      actor receive SwipeCard(0, card)
      expectNoMsg()

      actor receive Plug(0)
      actor.stateData mustEqual PluggedConnectors(Set(0))
      expectMsg(ConnectorActor.Plug)

      actor receive SwipeCard(0, card)
      expectMsg(ConnectorActor.SwipeCard(card))

      actor receive Plug(1)
      actor.stateData mustEqual PluggedConnectors(Set(0, 1))
      expectMsg(ConnectorActor.Plug)

      actor receive SwipeCard(1, card)
      expectMsg(ConnectorActor.SwipeCard(card))

      actor receive Unplug(0)
      actor.stateData mustEqual PluggedConnectors(Set(1))
      expectMsg(ConnectorActor.Unplug)

      actor receive Unplug(1)
      actor.stateData mustEqual NoData
      expectMsg(ConnectorActor.Unplug)
    }


    class ChargerActorScope
      extends TestKit(ActorSystem("test"))
      with Scope {
      val numberOfConnectors = 2
      val service = mock[BosService]
      val actor = TestFSMRef(new TestChargerActor)
      val card = "3E60A5E2"

      class TestChargerActor extends ChargerActor(service, numberOfConnectors) {
        override def startConnector(c: Int) {}
        override def connector(c: Int) = testActor
      }
    }
  }
}