package com.thenewmotion.chargenetwork.ocpp.charger.json

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.json4s.JValue
import com.thenewmotion.ocpp.messages.ChargePointReq

class DefaultOcppConnectionSpec extends SpecificationWithJUnit {

  "DefaultOcppConnection" should {

    "respond to requests with the same call ID" in new DefaultOcppConnectionScope {

    }
  }

  private trait DefaultOcppConnectionScope extends Scope {
    trait TestWebSocketConnection extends WebSocketConnection {
      def send(jval: JValue) = ???
    }

    val testConnection = new ChargePointOcppConnection with TestWebSocketConnection {
      def onRequest(request: ChargePointReq) = ???
      def onOcppError(err: OcppError) = ???

    }
  }
}
