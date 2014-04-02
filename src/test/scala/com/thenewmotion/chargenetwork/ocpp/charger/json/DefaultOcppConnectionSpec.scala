package com.thenewmotion.chargenetwork.ocpp.charger.json

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.thenewmotion.ocpp.messages._
import org.specs2.mock.Mockito
import scala.concurrent.{Await, Future, Promise}
import org.json4s.JsonDSL._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import com.thenewmotion.ocpp.json._
import com.thenewmotion.ocpp.messages.RemoteStopTransactionRes
import com.thenewmotion.ocpp.json.RequestMessage
import com.thenewmotion.ocpp.json.ResponseMessage
import com.thenewmotion.chargenetwork.ocpp.charger.json.OcppError

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


    "respond to requests with an error message if processing fails" in new DefaultOcppConnectionScope {
      onRequest.apply(any) returns Left(OcppError(PayloadErrorCode.FormationViolation, "aargh!"))

      testConnection.onSrpcMessage(srpcRemoteStopTransactionReq)

      awaitFirstSentMessage must beAnInstanceOf[ErrorResponseMessage]
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

    val testConnection = new ChargePointOcppConnectionComponent with SrpcConnectionComponent {
      val srpcConnection = new SrpcConnection {
        def send(msg: TransportMessage) = sentSrpcMessagePromise.success(msg)
      }
      val ocppConnection = new ChargePointOcppConnection

      def onRequest(request: ChargePointReq) = Future { DefaultOcppConnectionScope.this.onRequest(request) }
      def onOcppError(err: OcppError) = DefaultOcppConnectionScope.this.onError(err)
    }
  }
}
