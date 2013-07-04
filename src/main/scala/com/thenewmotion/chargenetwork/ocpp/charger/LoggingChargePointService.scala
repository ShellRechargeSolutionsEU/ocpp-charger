package com.thenewmotion.chargenetwork.ocpp.charger

import com.thenewmotion.ocpp._
import org.joda.time.DateTime
import com.thenewmotion.ocpp.GetDiagnosticsRetries
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Implementation of ChargePointService that just logs each method call on it and does nothing else
 */
object LoggingChargePointService extends ChargePointService with Logging {
  private def logCommand(commandName: String, parameters: Map[String, Any] = Map.empty) = {
    def formatParameter(param: (String, Any)) = param._1 + "=" + param._2
    val parameterString = parameters.map(formatParameter).mkString(", ")
    logger.info(s"Command received from central system: ${commandName} with parameters ${parameterString}")
  }

  def reset(resetType: ResetType.Value): Boolean = {
    logCommand("Reset", Map("resetType" -> resetType))
    false
  }

  def getDiagnostics(location: _root_.com.thenewmotion.ocpp.Uri, startTime: Option[DateTime],
                     stopTime: Option[DateTime], retries: Option[GetDiagnosticsRetries]): Option[String] = {
    logCommand("GetDiagnostics", Map("location" -> location,
      "startTime" -> startTime,
      "stopTime" -> stopTime,
      "retries" -> retries))
    None
  }

  def unlockConnector(connector: ConnectorScope): Boolean = {
    logCommand("UnlockConnector", Map("connector" -> connector))
    false
  }

  def remoteStopTransaction(transactionId: Int): Boolean = {
    logCommand("RemoteStopTransaction", Map("transactionId" -> transactionId))
    false
  }

  def remoteStartTransaction(idTag: IdTag, connector: Option[ConnectorScope]): Boolean = {
    logCommand("RemoteStartTransaction", Map("idTag" -> idTag, "connector" -> connector))
    false
  }

  def changeConfiguration(key: String, value: String): ConfigurationStatus.Value = {
    logCommand("ChangeConfiguration", Map("key" -> key, "value" -> value))
    ConfigurationStatus.NotSupported
  }

  @scala.throws[ActionNotSupportedException]
  def getConfiguration(keys: List[String]): Configuration = {
    logCommand("GetConfiguration", Map("keys" -> keys))
    Configuration(Nil, keys)
  }

  def changeAvailability(scope: Scope, availabilityType: AvailabilityType.Value): AvailabilityStatus.Value = {
    logCommand("ChangeAvailability", Map("scope" -> scope, "availabilityType" -> availabilityType))
    AvailabilityStatus.Rejected
  }

  def clearCache: Boolean = {
    logCommand("ClearCache")
    false
  }

  def updateFirmware(retrieveDate: DateTime, location: Uri, retries: Option[Int], retryInterval: Option[Int]) {
    logCommand("UpdateFirmware", Map("retrieveDate" -> retrieveDate,
      "location" -> location,
      "retries" -> retries,
      "retryInterval" -> retryInterval))
  }

  @scala.throws[ActionNotSupportedException]
  def sendLocalList(updateType: UpdateType.Value, listVersion: AuthListSupported,
                    localAuthorisationList: List[AuthorisationData], hash: Option[String]): UpdateStatus.Value = {
    logCommand("SendLocalList", Map("updateType" -> updateType,
      "listVersion" -> listVersion,
      "localAuthorisationList" -> localAuthorisationList,
      "hash" -> hash))
    UpdateStatus.NotSupportedValue
  }

  @scala.throws[ActionNotSupportedException]
  def getLocalListVersion: AuthListVersion = {
    logCommand("GetLocalListVersion")
    AuthListNotSupported
  }

  @scala.throws[ActionNotSupportedException]
  def dataTransfer(vendorId: String, messageId: Option[String], data: Option[String]): DataTransferResponse = {
    logCommand("DataTransfer", Map("vendorId" -> vendorId,
      "messageId" -> messageId,
      "data" -> data))
    DataTransferResponse(DataTransferStatus.UnknownVendorId)
  }

  @scala.throws[ActionNotSupportedException]
  def reserveNow(connector: Scope, expiryDate: DateTime, idTag: _root_.com.thenewmotion.ocpp.IdTag,
                 parentIdTag: Option[String], reservationId: Int): Reservation.Value = {
    logCommand("ReserveNow", Map("connector" -> connector,
      "expiryDate" -> expiryDate,
      "idTag" -> idTag,
      "parentIdTag" -> parentIdTag,
      "reservationId" -> reservationId))
    Reservation.Rejected
  }

  @scala.throws[ActionNotSupportedException]
  def cancelReservation(reservationId: Int): Boolean = {
    logCommand("CancelReservation", Map("reservationId" -> reservationId))
    false
  }
}

