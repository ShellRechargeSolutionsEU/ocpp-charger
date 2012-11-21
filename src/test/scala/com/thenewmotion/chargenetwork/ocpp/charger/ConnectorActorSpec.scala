package com.thenewmotion.chargenetwork
package ocpp.charger

import org.specs2.mutable.SpecificationWithJUnit
import akka.testkit.{TestFSMRef, TestKit}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import akka.actor.ActorSystem
import ConnectorActor._
import com.thenewmotion.chargenetwork.{ocpp => xb}

/**
 * @author Yaroslav Klymko
 */
class ConnectorActorSpec extends SpecificationWithJUnit with Mockito {
  "ConnectorActor" should {

    "become occupied when plug connected" in new ConnectorActorScope {
      actor.stateName mustEqual Available
      actor receive Plug
      actor.stateName mustEqual Connected
      there was one(service).notification(xb.Occupied)
    }

    "become available when plug disconnected" in new ConnectorActorScope {
      actor.setState(stateName = Connected)
      actor receive Unplug
      actor.stateName mustEqual Available
      there was one(service).notification(xb.Available)
    }

    "not start charging when card declined" in new ConnectorActorScope {
      actor.setState(stateName = Connected)
      service.authorize(rfid) returns false

      actor receive SwipeCard(rfid)

      actor.stateName mustEqual Connected
      there was one(service).authorize(rfid)
    }

    "start charging when card accepted" in new ConnectorActorScope {
      actor.setState(stateName = Connected)
      service.authorize(rfid) returns true
      service.startSession(rfid) returns 12345

      actor receive SwipeCard(rfid)

      actor.stateName mustEqual Charging
      actor.stateData mustEqual ChargingData(12345)

      there was one(service).authorize(rfid)
      there was one(service).startSession(rfid)
    }

    "continue charging when card declined" in new ConnectorActorScope {
      actor.setState(stateName = Charging, stateData = ChargingData(12345))
      service.authorize(rfid) returns false

      actor receive SwipeCard(rfid)
      actor.stateName mustEqual Charging

      there was one(service).authorize(rfid)
    }

    "stop charging when card accepted" in new ConnectorActorScope {
      actor.setState(stateName = Charging, stateData = ChargingData(12345))
      service.authorize(rfid) returns true
      service.stopSession(Some(rfid), 12345) returns true

      actor receive SwipeCard(rfid)
      actor.stateName mustEqual Connected
      actor.stateData mustEqual NoData

      there was one(service).authorize(rfid)
      there was one(service).stopSession(Some(rfid), 12345)
    }

    "not stop charging when card declined" in new ConnectorActorScope {
      actor.setState(stateName = Charging, stateData = ChargingData(12345))
      service.authorize(rfid) returns false

      actor receive SwipeCard(rfid)
      actor.stateName mustEqual Charging
      there was one(service).authorize(rfid)
      there was no(service).stopSession(Some(rfid), 12345)
    }

    "stop charging on termination" in new ConnectorActorScope {
      actor.setState(stateName = Charging, stateData = ChargingData(12345))
      system.shutdown()
      system.awaitTermination()

      there was one(service).stopSession(None, 12345)
    }
  }

  class ConnectorActorScope
    extends TestKit(ActorSystem("test"))
    with Scope {
    val service = mock[BosConnectorService]
    val actor = TestFSMRef(new ConnectorActor(service))
    val rfid = "rfid"
  }
}