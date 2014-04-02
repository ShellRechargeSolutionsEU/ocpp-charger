package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.json.{TransportMessageParser, TransportMessage}
import org.json4s.JValue
import com.typesafe.scalalogging.slf4j.Logging

trait SrpcComponent {
  trait SrpcConnection {
    def send(msg: TransportMessage)
  }

  def srpcConnection: SrpcConnection

  def onSrpcMessage(msg: TransportMessage)
}

trait DefaultSrpcComponent extends SrpcComponent with Logging {
  this: WebSocketComponent =>

  class DefaultSrpcConnection extends SrpcConnection {
    def send(msg: TransportMessage) {
      webSocketConnection.send(TransportMessageParser.writeJValue(msg))
    }
  }

  // TODO: log errors somehow
  def onMessage(jval: JValue) = onSrpcMessage(TransportMessageParser.parse(jval))

  def onError(ex: Throwable) = logger.error("WebSocket error", ex)
}


