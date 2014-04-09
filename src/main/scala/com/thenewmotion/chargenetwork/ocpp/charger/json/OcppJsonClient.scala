package com.thenewmotion.chargenetwork.ocpp.charger.json

import java.net.URI
import com.thenewmotion.ocpp.messages.{ChargePointRes, ChargePointReq, CentralSystemRes, CentralSystemReq}
import io.backchat.hookup.HookupClientConfig
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

abstract class OcppJsonClient(chargerId: String, centralSystemUri: URI)
  extends OcppEndpoint[CentralSystemReq, CentralSystemRes, ChargePointReq, ChargePointRes] {

  val ocppStack = new ChargePointOcppConnectionComponent with DefaultSrpcComponent with HookupClientWebSocketComponent {
    val webSocketConnection = new HookupClientWebSocketConnection(chargerId, new HookupClientConfig(uri = centralSystemUri))
    val srpcConnection = new DefaultSrpcConnection
    val ocppConnection = new ChargePointOcppConnection

    override def onRequest(req: ChargePointReq): Future[Either[OcppError, ChargePointRes]] =
      OcppJsonClient.this.onRequest(req).map(Right(_))

    override def onOcppError(error: OcppError): Unit = OcppJsonClient.this.onError(error)

    // TODO: link up onDisconnect
  }

  def send(req: CentralSystemReq): Future[CentralSystemRes] = {
    ocppStack.ocppConnection.sendRequest(req) map {
      case Right(res) => res
        // TODO: use OcppException thing
      case Left(err) => throw new RuntimeException(s"OCPP error: $err")
    }
  }
}
