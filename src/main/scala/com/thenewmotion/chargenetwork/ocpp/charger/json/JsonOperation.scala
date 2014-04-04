package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages._
import com.thenewmotion.ocpp.messages.centralsystem.CentralSystemAction
import com.thenewmotion.ocpp.messages.chargepoint.ChargePointAction
import scalax.RichEnum
import scala.reflect._
import com.thenewmotion.ocpp.json.JsonDeserializable
import com.thenewmotion.ocpp.json.JsonDeserializable._
import org.json4s._

class JsonOperation[REQ <: Req : JsonDeserializable : ClassTag, RES <: Res : JsonDeserializable](val action: Enumeration#Value) {
  def deserializeReq(jval: JValue): REQ = jsonDeserializable[REQ].deserializeV15(jval)

  def deserializeRes(jval: JValue): RES = jsonDeserializable[RES].deserializeV15(jval)

  private val reqClass = classTag[REQ].runtimeClass

  def matchesRequest(req: Req): Boolean = reqClass.isInstance(req)
}

trait JsonOperations[REQ <: Req, RES <: Res] {
  sealed trait LookupResult
  case object NotImplemented extends LookupResult
  case object Unsupported extends LookupResult
  case class Supported(op: JsonOperation[_ <: REQ, _ <: RES]) extends LookupResult

  def enum: Enumeration

  def operations: Traversable[JsonOperation[_ <: REQ, _ <: RES]]

  def actionForRequestObject(req: REQ): Option[Enumeration#Value] =
    operations.find(_.matchesRequest(req)).map(_.action)

  def jsonOpForAction(action: Enumeration#Value): Option[JsonOperation[_ <: REQ, _ <: RES]] =
    operations.find(_.action == action)

  def jsonOpForActionName(operationName: String): LookupResult = {
    enum.withNameOpt(operationName) match {
      case Some(action) => jsonOpForAction(action) match {
        case None => Unsupported
        case Some(jsonAction) => Supported(jsonAction)
      }
      case None         => NotImplemented
    }
  }
}


object CentralSystemOperations extends JsonOperations[CentralSystemReq, CentralSystemRes] {

  import CentralSystemAction._

  val enum = CentralSystemAction

  val operations: Traversable[JsonOperation[_ <: CentralSystemReq, _ <: CentralSystemRes]] = List(
    new JsonOperation[AuthorizeReq, AuthorizeRes](Authorize),
    new JsonOperation[BootNotificationReq, BootNotificationRes](BootNotification),
    new JsonOperation[DiagnosticsStatusNotificationReq, DiagnosticsStatusNotificationRes.type](DiagnosticsStatusNotification),
    new JsonOperation[FirmwareStatusNotificationReq, FirmwareStatusNotificationRes.type](FirmwareStatusNotification),
    new JsonOperation[HeartbeatReq.type, HeartbeatRes](Heartbeat),
    new JsonOperation[MeterValuesReq, MeterValuesRes.type](MeterValues),
    new JsonOperation[StartTransactionReq, StartTransactionRes](StartTransaction),
    new JsonOperation[StatusNotificationReq, StatusNotificationRes.type](StatusNotification),
    new JsonOperation[StopTransactionReq, StopTransactionRes](StopTransaction))

}

object ChargePointOperations extends JsonOperations[ChargePointReq, ChargePointRes] {
  import ChargePointAction._

  val enum = ChargePointAction

  val operations: Traversable[JsonOperation[_ <: ChargePointReq, _ <: ChargePointRes]] = List(
    new JsonOperation[CancelReservationReq, CancelReservationRes](CancelReservation),
    new JsonOperation[ChangeAvailabilityReq, ChangeAvailabilityRes](ChangeAvailability),
    new JsonOperation[ChangeConfigurationReq, ChangeConfigurationRes](ChangeConfiguration),
    new JsonOperation[ClearCacheReq.type, ClearCacheRes](ClearCache),
    new JsonOperation[GetConfigurationReq, GetConfigurationRes](GetConfiguration),
    new JsonOperation[GetDiagnosticsReq, GetDiagnosticsRes](GetDiagnostics),
    new JsonOperation[GetLocalListVersionReq.type, GetLocalListVersionRes](GetLocalListVersion),
    new JsonOperation[RemoteStartTransactionReq, RemoteStartTransactionRes](RemoteStartTransaction),
    new JsonOperation[RemoteStopTransactionReq, RemoteStopTransactionRes](RemoteStopTransaction),
    new JsonOperation[ReserveNowReq, ReserveNowRes](ReserveNow),
    new JsonOperation[ResetReq, ResetRes](Reset),
    new JsonOperation[SendLocalListReq, SendLocalListRes](SendLocalList),
    new JsonOperation[UnlockConnectorReq, UnlockConnectorRes](UnlockConnector),
    new JsonOperation[UpdateFirmwareReq, UpdateFirmwareRes.type](UpdateFirmware))

}

class UnsupportedOperationException(operationName: String)
  extends RuntimeException(s"Unsupported OCPP operation $operationName")

class NotImplementedOperationException(operationName: String)
  extends RuntimeException(s"Unknown OCPP operation $operationName")
