package com.thenewmotion.chargenetwork.ocpp.charger.json

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.thenewmotion.ocpp.messages._
import org.specs2.mock.Mockito
import scala.concurrent.{Await, Future, Promise}
import org.json4s.JsonDSL._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.thenewmotion.ocpp.json._
import com.thenewmotion.ocpp.messages.RemoteStopTransactionRes
import com.thenewmotion.ocpp.json.RequestMessage
import com.thenewmotion.ocpp.json.ResponseMessage

class DefaultOcppConnectionSpec extends SpecificationWithJUnit with Mockito {

  "DefaultOcppConnection" should {

    "respond to requests with a response message" in new DefaultOcppConnectionScope {
      onRequest.apply(any) returns Right(RemoteStopTransactionRes(true))

      testConnection.onSrpcMessage(srpcRemoteStopTransactionReq)

      awaitFirstSentMessage must beAnInstanceOf[ResponseMessage]
    }

    "respond to requests with the same call ID" in new DefaultOcppConnectionScope {
      onRequest.apply(any) returns Right(RemoteStopTransactionRes(true))

      testConnection.onSrpcMessage(srpcRemoteStopTransactionReq)

      awaitFirstSentMessage must beLike {
        case ResponseMessage(callId, _) => callId mustEqual srpcRemoteStopTransactionReq.callId
      }
    }

    "respond to requests with an error message if processing returns an OCPP error" in new DefaultOcppConnectionScope {
      onRequest.apply(any) returns Left(OcppError(PayloadErrorCode.FormationViolation, "aargh!"))

      testConnection.onSrpcMessage(srpcRemoteStopTransactionReq)

      awaitFirstSentMessage must beAnInstanceOf[ErrorResponseMessage]
    }

    "respond to requests with an error message if processing throws" in new DefaultOcppConnectionScope {
      onRequest.apply(any) throws new RuntimeException("bork")

      testConnection.onSrpcMessage(srpcRemoteStopTransactionReq)

      awaitFirstSentMessage must beLike {
        case ErrorResponseMessage(callId, errorCode, description, details) =>
          (callId, errorCode) mustEqual (srpcRemoteStopTransactionReq.callId, PayloadErrorCode.InternalError)
      }
    }

    "give incoming responses back to the caller" in new DefaultOcppConnectionScope {
      val futureResponse = testConnection.ocppConnection.sendRequest(HeartbeatReq)

      val callId = awaitFirstSentMessage.asInstanceOf[RequestMessage].callId
      val srpcHeartbeatRes = ResponseMessage(callId, "currentTime" -> "2014-03-31T14:00:00Z")

      testConnection.onSrpcMessage(srpcHeartbeatRes)

      Await.result(futureResponse, FiniteDuration(1, "second")) must beRight
    }

    "return an error to the caller when a request is responded to with an error message" in new DefaultOcppConnectionScope {
      val futureResponse = testConnection.ocppConnection.sendRequest(HeartbeatReq)

      val callId = awaitFirstSentMessage.asInstanceOf[RequestMessage].callId
      val errorRes = ErrorResponseMessage(callId, PayloadErrorCode.SecurityError, "Hee! Da mag nie!", "allowed" -> "no")

      testConnection.onSrpcMessage(errorRes)

      val expected = Left(OcppError(PayloadErrorCode.SecurityError, "Hee! Da mag nie!"))
      Await.result(futureResponse, FiniteDuration(1, "seconds")) mustEqual expected
    }

  }

  private trait DefaultOcppConnectionScope extends Scope {
    val srpcRemoteStopTransactionReq =
      RequestMessage("test-call-id", "RemoteStopTransaction", "transactionId" -> 3)

    val onRequest = mock[ChargePointReq => Either[OcppError, ChargePointRes]]
    val onError = mock[OcppError => Unit]

    val sentSrpcMessagePromise = Promise[TransportMessage]()
    val sentSrpcMessage: Future[TransportMessage] = sentSrpcMessagePromise.future

    def awaitFirstSentMessage: TransportMessage = Await.result(sentSrpcMessage, FiniteDuration(1, "second"))

    val testConnection = new ChargePointOcppConnectionComponent with SrpcComponent {
      val srpcConnection = new SrpcConnection {
        def send(msg: TransportMessage) = sentSrpcMessagePromise.success(msg)
      }
      val ocppConnection = new ChargePointOcppConnection

      def onRequest(request: ChargePointReq) = Future { DefaultOcppConnectionScope.this.onRequest(request) }
      def onOcppError(err: OcppError) = DefaultOcppConnectionScope.this.onError(err)
    }
  }
}
