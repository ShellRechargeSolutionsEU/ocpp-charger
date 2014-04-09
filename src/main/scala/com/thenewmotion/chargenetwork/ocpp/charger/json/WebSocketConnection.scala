package com.thenewmotion.chargenetwork.ocpp.charger.json

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.JsonParser
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.net.URI
import io.backchat.hookup._
import io.backchat.hookup.HookupClient.Receive
import io.backchat.hookup.{HookupClientConfig, JsonMessage, TextMessage, Connected}

trait WebSocketComponent {
  trait WebSocketConnection {
    /**
     * Send a JSON message to the other party
     *
     * To be implemented by children implementing the WebSocket communication. To
     * be called by users of the WebSocket connectivity.
     */
    def send(msg: JValue): Unit

  }

  def webSocketConnection: WebSocketConnection

  /**
   * Called when a new JSON message arrives.
   *
   * To be implemented by children using the WebSocket connectivity. To be called by children implementing the
   * WebSocket connectivity.
   */
  def onMessage(msg: JValue)

  /**
   * Called when a WebSocket error occurs
   * @param e
   */
  def onError(e: Throwable)
}

trait DummyWebSocketComponent extends WebSocketComponent {

  class MockWebSocketConnection extends WebSocketConnection with Logging {
    def send(msg: JValue) = {
      val string = Serialization.write(msg)(DefaultFormats)
      logger.info(s"Sending $string")
    }

    private val testGetConfigurationReq = JsonParser.parse( """[2, "test-call-id", "GetConfiguration", { "key": [ "KVCBX_PROFILE" ] }]""")
    private val testReserveNowReq: JValue = JsonParser.parse( """[2, "test-call-id-2", "ReserveNow", { "connectorId": 0, "expiryDate": "2013-02-01T15:09:18Z", "idTag": "044943121F1D80", "parentIdTag": "", "reservationId": 0 }]""")
    WebSocketClientApp.system.scheduler.scheduleOnce(FiniteDuration(500, "milliseconds"))(onMessage(testGetConfigurationReq))
    WebSocketClientApp.system.scheduler.scheduleOnce(FiniteDuration(1, "second"))(onMessage(testReserveNowReq))
  }
}

trait HookupClientWebSocketComponent extends WebSocketComponent {

  private val ocppProtocol = "ocpp1.5"

  class HookupClientWebSocketConnection(chargerId: String, config: HookupClientConfig) extends WebSocketConnection with Logging {

    private val hookupClientConfig = HookupClientConfig(uri = uriWithChargerId(config.uri, chargerId))

    private val client = new DefaultHookupClient(hookupClientConfig) {
      def receive: Receive = {
        case Connected => logger.debug("WebSocket connection connected to {}", hookupClientConfig.uri)
        case Disconnected(_) => logger.debug("WebSocket connection disconnected from {}", hookupClientConfig.uri)
        case JsonMessage(jval) =>
          logger.debug("Received JSON message {}", jval)
          onMessage(jval)
        case TextMessage(txt) =>
          logger.debug("Received non-JSON message \"{}\"", txt)
          onError(new Exception(s"Invalid JSON received: $txt"))
        case e@Error(maybeEx) =>
          logger.debug("Received error {}", e)
          val exception = maybeEx getOrElse new Exception("WebSocket error received without more information")
          onError(exception)
      }
    }

    private def uriWithChargerId(base: URI, chargerId: String): URI = {
      val pathWithChargerId = base.getPath + s"/$chargerId"
      new URI(base.getScheme, base.getUserInfo, base.getHost, base.getPort, pathWithChargerId, base.getQuery,
        base.getFragment)
    }

    def send(jval: JValue) = {
      logger.debug("Sending with Hookup: {}", jval)
      client.send(jval)
    }

    client.connect(ocppProtocol)
  }
}
