package com.thenewmotion.chargenetwork.ocpp.charger.json

import java.net.URI
import com.thenewmotion.ocpp.messages._
import io.backchat.hookup.HookupClientConfig
import scala.concurrent.Future
import com.thenewmotion.ocpp.messages.centralsystem.CentralSystemReqRes
import io.backchat.hookup.HookupClientConfig
import com.thenewmotion.chargenetwork.ocpp.charger.json.OcppError

// TODO:
// * foutafhandeling in send voor niet-bekende ops
// * importeren reqressen
abstract class OcppJsonClient(chargerId: String, centralSystemUri: URI)
  extends OcppEndpoint[CentralSystemReq, CentralSystemRes, ChargePointReq, ChargePointRes] {

  val ocppStack = new ChargePointOcppConnectionComponent with DefaultSrpcComponent with HookupClientWebSocketComponent {
    val webSocketConnection = new HookupClientWebSocketConnection(chargerId, new HookupClientConfig(uri = centralSystemUri))
    val srpcConnection = new DefaultSrpcConnection
    val ocppConnection = new ChargePointOcppConnection

    override def onRequest(req: ChargePointReq): Future[ChargePointRes] =
      OcppJsonClient.this.onRequest(req)

    override def onOcppError(error: OcppError): Unit = OcppJsonClient.this.onError(error)

    override def onDisconnect(): Unit = OcppJsonClient.this.onDisconnect
  }

  def send[REQ <: CentralSystemReq, RES <: CentralSystemRes](req: REQ)(implicit reqRes: ReqRes[REQ, RES]): Future[RES] =
    ocppStack.ocppConnection.sendRequest(req)

  def close() = ocppStack.webSocketConnection.close()
}
