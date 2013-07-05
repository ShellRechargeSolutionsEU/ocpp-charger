package com.thenewmotion.chargenetwork.ocpp.charger

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import com.thenewmotion.ocpp.spray.ChargerInfo
import com.thenewmotion.ocpp.{Version, Uri}

class OcppChargerSpec extends SpecificationWithJUnit with Mockito {
  "OcppCharger" should {
    "return no charge point service if one is requested for another ID" in {
      val chargerInfo = mock[ChargerInfo]
      chargerInfo.chargerId returns "charger-id-1"
      val ocppCharger = new OcppCharger("charger-id-2", 2, Version.V15,
                                        new Uri("http://example.com/centralsystem"), 12345)

      ocppCharger.serviceForCharger(chargerInfo) must beNone
    }

    "return charge point service if one is requested for its own charger ID" in {
      val chargerInfo = mock[ChargerInfo]
      chargerInfo.chargerId returns "charger-id"
      val ocppCharger = new OcppCharger("charger-id", 2, Version.V15,
                                        new Uri("http://example.com/centralsystem"), 12345)

      ocppCharger.serviceForCharger(chargerInfo) must beSome
    }
  }
}
