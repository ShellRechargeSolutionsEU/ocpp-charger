package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.json._
import v15.Ocpp15J
import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}
import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A component that, when mixed into something that is also an SRPC component, can send and receive OCPP-JSON messages
 * over that SRPC connection.
 *
 * @tparam OUTREQ Type of outgoing requests
 * @tparam INRES Type of incoming responses
 * @tparam INREQ Type of incoming requests
 * @tparam OUTRES Type of outgoing responses
 */
trait OcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res] {
  this: SrpcComponent =>

  trait OcppConnection {
    /** Send an outgoing OCPP request */
    def sendRequest(req: OUTREQ): Future[Either[OcppError, INRES]]

    /** Handle an incoming SRPC message */
    def onSrpcMessage(msg: TransportMessage)
  }

  def ocppConnection: OcppConnection

  // XXX or shouldn't we allow application code to send arbitrary OCPP errors?
  def onRequest(req: INREQ): Future[Either[OcppError, OUTRES]]
  def onOcppError(error: OcppError)
}

trait DefaultOcppConnectionComponent[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res]
  extends OcppConnectionComponent[OUTREQ, INRES, INREQ, OUTRES] {

  this: SrpcComponent =>

  trait DefaultOcppConnection extends OcppConnection with Logging {
    /** The operations that the other side can request from us */
    val ourOperations: JsonOperations[INREQ, OUTRES]
    val theirOperations: JsonOperations[OUTREQ, INRES]

    private val callIdGenerator = CallIdGenerator()

    case class OutstandingRequest(operation: JsonOperation[_ <: OUTREQ, _ <: INRES],
                                  responsePromise: Promise[Either[OcppError, _ <: INRES]])

    private val callIdCache: mutable.Map[String, OutstandingRequest] = mutable.Map()

    def onSrpcMessage(msg: TransportMessage) {
      logger.debug("Incoming SRPC message: {}", msg)

      msg match {
        case req: RequestMessage => handleIncomingRequest(req)
        case res: ResponseMessage => handleIncomingResponse(res)
        case err: ErrorResponseMessage => handleIncomingError(err)
      }
    }

    private def handleIncomingRequest(req: RequestMessage) {
      import ourOperations._

      def respondWithError(errCode: PayloadErrorCode.Value, description: String) =
        srpcConnection.send(ErrorResponseMessage(req.callId, errCode, description))

      val opName = req.procedureName
      jsonOpForActionName(opName) match {
        case NotImplemented => respondWithError(PayloadErrorCode.NotImplemented, s"Unknown operation $opName")
        case Unsupported => respondWithError(PayloadErrorCode.NotSupported, s"We do not support $opName")
        case Supported(operation) =>
          val ocppMsg = operation.deserializeReq(req.payload)
          val responseSrpc = onRequest(ocppMsg) recover {
            case e: Throwable =>
              logger.error(s"OCPP request processing for ${req.procedureName} threw exception", e)
              Left(OcppError(PayloadErrorCode.InternalError, "Unexpected error processing request"))
          } map {
            responseToSrpc(req.callId, _)
          }

          responseSrpc onComplete {
            case Success(json) => srpcConnection.send(json)
            case Failure(e) =>
              logger.error("OCPP response future failed for {} with call ID {}. This ought to be impossible.",
                opName, req.callId)
          }
      }
    }

    private def responseToSrpc[REQ <: INREQ, RES <: OUTRES](callId: String, response: Either[OcppError, OUTRES]): TransportMessage =
      response match {
        case Left(error) => ErrorResponseMessage(callId, error.error, error.description)
        case Right(res) => ResponseMessage(callId, Ocpp15J.serialize(res))
      }


    private def handleIncomingResponse(res: ResponseMessage) {
      callIdCache.get(res.callId) match {
        case None =>
          logger.info("Received response for no request: {}", res)
        case Some(OutstandingRequest(op, resPromise)) =>
          val response = op.deserializeRes(res.payload)
          resPromise.success(Right(response))
      }
    }

    private def handleIncomingError(err: ErrorResponseMessage) = err match {
      case ErrorResponseMessage(callId, errCode, description, details) =>
        callIdCache.get(callId) match {
          case None => logger.error("Received OCPP error with unrecognized call ID {}: {} {}",
            callId, errCode, description)
          case Some(OutstandingRequest(operation, futureResponse)) =>
            futureResponse success Left(OcppError(errCode, description))
        }
    }

    def sendRequest(req: OUTREQ): Future[Either[OcppError, INRES]] = {
      val callId = callIdGenerator.next()
      val operationName = getProcedureName(req)
      val responsePromise = Promise[Either[OcppError, INRES]]()

      theirOperations.jsonOpForActionName(operationName) match {
        case theirOperations.Supported(operation) =>
          callIdCache.put (callId, OutstandingRequest(operation, responsePromise))
          // TODO have a way to not hardcode the OCPP version number when (de)serializing OCPP
          srpcConnection.send (RequestMessage (callId, getProcedureName (req), Ocpp15J.serialize (req) ) )
          responsePromise.future
        case _ => throw new Exception(s"Tried to send unsupported OCPP request $operationName")
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

