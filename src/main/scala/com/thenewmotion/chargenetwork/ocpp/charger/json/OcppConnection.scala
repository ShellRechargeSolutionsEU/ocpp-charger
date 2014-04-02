package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json._
import v15.Ocpp15J
import scala.concurrent.Future
import scala.util.{Success, Failure}
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

trait OcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res] {
  this: SrpcComponent =>

  trait OcppConnection {
    /** Send an outgoing OCPP request */
    def sendRequest(req: OUTREQ): Future[Either[OcppError, INRES]]

    /** Handle an incoming SRPC message */
    def onSrpcMessage(msg: TransportMessage)
  }

  def ocppConnection: OcppConnection

  def onRequest(req: INREQ): Future[Either[OcppError, OUTRES]]
  def onOcppError(error: OcppError)
}

trait DefaultOcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res]
  extends OcppConnectionComponent[OUTREQ, INRES, INREQ, OUTRES] {

  this: SrpcComponent =>

  trait DefaultOcppConnection extends OcppConnection with Logging {
    /** The operations that the other side can request from us */
    def ourOperations: JsonOperations[INREQ, OUTRES]
    def theirOperations: JsonOperations[OUTREQ, INRES]

    private val callIdGenerator = CallIdGenerator()

    private val callIdCache: mutable.Map[String, Class[Message]] = mutable.Map()

    def onSrpcMessage(msg: TransportMessage) {
      msg match {
        case req: RequestMessage =>
          val op = ourOperations.jsonOpForActionName(req.procedureName) // TODO handle exn
        val ocppMsg = op.deserializeReq(req.payload)
          val responseSrpc = onRequest(ocppMsg) map {
            responseToSrpc(req.callId, _)
          }

          responseSrpc onComplete {
            case Success(json) => srpcConnection.send(json)
            case Failure(e) => // TODO
          }
        case res: ResponseMessage => logger.info(s"Got a response: $res")
        case err: ErrorResponseMessage => logger.info(s"Got an error: $err")
        // TODO: handle exception
      }
    }

    private def responseToSrpc[REQ <: INREQ, RES <: OUTRES](callId: String, response: Either[OcppError, OUTRES]): TransportMessage =
      response match {
        case Left(error) => ErrorResponseMessage(callId, error.error, error.description)
        case Right(res) => ResponseMessage(callId, Ocpp15J.serialize(res))
      }

    def sendRequest(req: OUTREQ) = {
      val callId = callIdGenerator.next()
      srpcConnection.send(RequestMessage(callId, getProcedureName(req), Ocpp15J.serialize(req)))
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

  def onSrpcMessage(msg: TransportMessage) = ocppConnection.onSrpcMessage(msg)
}

trait ChargePointOcppConnectionComponent
  extends DefaultOcppConnectionComponent[CentralSystemReq, CentralSystemRes, ChargePointReq, ChargePointRes] {
  this: SrpcComponent =>

  class ChargePointOcppConnection extends DefaultOcppConnection {
    val ourOperations = ChargePointOperations
    val theirOperations = CentralSystemOperations
  }
}

