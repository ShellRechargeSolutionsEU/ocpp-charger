package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json._
import centralsystem._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import com.thenewmotion.ocpp.messages.BootNotificationReq
import scala.Some
import java.net.URI

// problemen:
//  * Het is nu echt een zooi tussen synchroon / asynchroon
//    -> hoe doen we message-IDs?
//    -> moeten we met correlatie-ID's blijven werken tot op hoogste niveau? Of toch ergens futures?
//    -> futures?
//   => Dit geëmmer speelt al zolang ik aan OCPP werk. Neem maar gewoon kortste weg.
//   => TODO: see how this can be made an instance of the CentralSystem trait
//        * Need apply to return a future there too
//        * SOAP/JSON independent error type
//  * Foutafhandeling: wat is het type van een OCPP-fout?
//  More immediate TODOs:
//   * Version-independent way to call OCPP-J functionality
//   * Serializing to SRPC in one call
//   * Port to json4s
//
//  Client zelf: sturen en ontvangen (bestaat op zich met die client, alleen subscribe-functie toevoegen)
//  OCPP-endpoint: bevat ook toestand die correlatie-IDs bijhoudt. Methods als in dat CentralSystem dan

case class OcppError(error: PayloadErrorCode.Value, description: String)

trait WebSocketCentralSystem {
  def apply[REQ <: CentralSystemReq, RES <: CentralSystemRes](req: REQ)(implicit reqRes: ReqRes[REQ, RES]): Future[Either[RES, OcppError]]

  def subscribeRequestHandler(handler: PartialFunction[ChargePointReq, Any])

  def subscribeErrorHandler(handler: PartialFunction[OcppError, Any])
}

/*
class WebSocketCentralSystemImpl extends WebSocketCentralSystem with Logging {
  val client = new DefaultHookupClient(HookupClientConfig(new URI("ws://localhost:8080/ocppws/REFACHA"))) {
    def receive = {
      case TextMessage(s) =>
        logger.warn(s"Received non-JSON msg $s")
      case JsonMessage(x) =>
        logger.info(s"<<$x")
      case Disconnected(_) =>
        logger.warn("WebSocket dood")
    }
  }

  val callIdGenerator = CallIdGenerator()

  def apply[REQ <: CentralSystemReq, RES <: CentralSystemRes](req: REQ)(implicit reqRes: ReqRes[REQ, RES]): Future[Either[RES, OcppError]] = {
    val reqJson = TransportMessageParser.write(RequestMessage(callIdGenerator.next(), getProcedureName(req), Ocpp15J.serialize(req)))
    logger.info(s">>$reqJson")
    client.send(reqJson)

    Future.failed(new RuntimeException("NYI"))
  }

  private def getProcedureName(c: Any) = {
    c.getClass.getSimpleName.replaceFirst("Re[qs]\\$?$", "")
  }

  def subscribeRequestHandler(handler: PartialFunction[ChargePointReq, Any]) = ???
  def subscribeErrorHandler(handler: PartialFunction[OcppError, Any]) = ???
}
*/

object WebSocketClientApp extends App {
  val system = ActorSystem()

  val testReq = BootNotificationReq("Aap", "Schaap", Some("REFACHA"), None, None, None, None, None, None)
  /*
  val c = new WebSocketCentralSystemImpl

  c.client.connect("ocpp1.5") onSuccess {
    case _ => c.apply(testReq)
  }
  */

  val c = new ChargePointOcppConnectionComponent with HookupClientWebSocketComponent {
    lazy val uri = new URI("http://localhost:8080/ocppws")
    def chargerId = "REFACHA"

    def onRequest(req: ChargePointReq): Future[Either[OcppError, ChargePointRes]] = Future {
      req match {
        case rnr: ReserveNowReq => Left(OcppError(PayloadErrorCode.InternalError, "Problemen met de voedselvoorziening"))
        case gcr: GetConfigurationReq => Right(GetConfigurationRes(List(), List("aap")))
      }
    }

    def onOcppError(error: OcppError) = ???
  }

  c.ocppConnection.sendRequest(testReq)
}
