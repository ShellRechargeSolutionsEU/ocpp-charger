package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json._
import v15.Ocpp15J
import scala.concurrent.Future
import scala.util.{Success, Failure}
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.mutable
import org.json4s._
import scala.concurrent.ExecutionContext.Implicits.global

trait OcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res] {
  this: WebSocketComponent =>

  trait OcppConnection {
    /** Send an outgoing OCPP request */
    def sendRequest(req: OUTREQ): Future[Either[OcppError, INRES]]

    /** Handle an incoming JSON message */
    def onMessage(jval: JValue)
  }

  def ocppConnection: OcppConnection

  def onMessage(jval: JValue) = ocppConnection.onMessage(jval)

  def onRequest(req: INREQ): Future[Either[OcppError, OUTRES]]
  def onOcppError(error: OcppError)
}

trait DefaultOcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res]
  extends OcppConnectionComponent[OUTREQ, INRES, INREQ, OUTRES] {

  this: WebSocketComponent =>

  trait DefaultOcppConnection extends OcppConnection with Logging {
    /** The operations that the other side can request from us */
    def ourOperations: JsonOperations[INREQ, OUTRES]
    def theirOperations: JsonOperations[OUTREQ, INRES]

    private val callIdGenerator = CallIdGenerator()

    private val callIdCache: mutable.Map[String, Class[Message]] = mutable.Map()

    def onMessage(msg: JValue) {
      TransportMessageParser.parse(msg) match {
        case req: RequestMessage =>
          val op = ourOperations.jsonOpForActionName(req.procedureName) // TODO handle exn
        val ocppMsg = op.deserializeReq(req.payload)
          val responseJson = onRequest(ocppMsg) map {
            responseToJson(req.callId, op, _)
          }

          responseJson onComplete {
            case Success(json) => webSocketConnection.send(json)
            case Failure(e) => // TODO
          }
        case res: ResponseMessage => logger.info(s"Got a response: $res")
        case err: ErrorResponseMessage => logger.info(s"Got an error: $err")
        // TODO: handle exception
      }
    }

    private def responseToJson[REQ <: INREQ, RES <: OUTRES](callId: String, op: JsonOperation[REQ, RES], response: Either[OcppError, OUTRES]): JValue =
      TransportMessageParser.writeJValue {
        response match {
          case Left(error) => ErrorResponseMessage(callId, error.error, error.description)
          case Right(res) => ResponseMessage(callId, Ocpp15J.serialize(res))
        }
      }

    def sendRequest(req: OUTREQ) = {
      val callId = callIdGenerator.next()
      webSocketConnection.send(TransportMessageParser.writeJValue(RequestMessage(callId, getProcedureName(req), Ocpp15J.serialize(req))))
      Future {
        Left(OcppError(PayloadErrorCode.NotImplemented, "We don't handle responses yet :)"))
      }
    }

    private def getProcedureName(c: Message) = {
      c.getClass.getSimpleName.replaceFirst("Re[qs]\\$?$", "")
    }

  }

  def onRequest(req: INREQ): Future[Either[OcppError, OUTRES]]
  def onOcppError(error: OcppError): Unit

  def onError(e: Throwable) = ???
}

trait ChargePointOcppConnectionComponent
  extends DefaultOcppConnectionComponent[CentralSystemReq, CentralSystemRes, ChargePointReq, ChargePointRes] {
  this: WebSocketComponent =>

  def ocppConnection = new DefaultOcppConnection {
    val ourOperations = ChargePointOperations
    val theirOperations = CentralSystemOperations
  }
}

