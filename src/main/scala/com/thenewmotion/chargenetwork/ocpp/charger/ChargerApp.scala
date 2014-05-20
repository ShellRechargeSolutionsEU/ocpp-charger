package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.Props
import com.thenewmotion.ocpp.Version
import java.net.URI
import org.rogach.scallop._
import java.util.Locale

/**
 * @author Yaroslav Klymko
 */
object ChargerApp {

  def main(args: Array[String]) {
    object config extends ScallopConf(args) {
      val chargerId = opt[String]("id", descr = "Charge point ID of emulated charge point", default = Some("00055103978E"))
      val numberOfConnectors = opt[Int]("connectors", descr = "Number of connectors of emulated charge point", default = Some(2))
      val passId = opt[String]("pass-id", descr = "RFID of pass to try to start sessions with", default = Some("3E60A5E2"))
      val protocolVersion = opt[String]("protocol-version", descr = "OCPP version (either \"1.2\" or \"1.5\"", default = Some("1.5"))
      val connectionType = opt[String]("connection-type", descr = "whether to use WebSocket/JSON or HTTP/SOAP (either  \"json\" or \"soap\")", default = Some("json"))
      val listenPort = opt[Short]("listen", descr = "TCP port to listen on for remote commands", default = Some(8084.toShort))
      val chargeServerUrl = trailArg[String](descr = "Charge server URL base (without trailing slash)", default = Some("http://127.0.0.1:8081/ocppws"))
    }

    val version = try {
      Version.withName(config.protocolVersion())
    } catch {
      case e: NoSuchElementException => sys.error(s"Unknown protocol version ${config.protocolVersion()}")
    }

    val connectionType: ConnectionType = config.connectionType().toLowerCase(Locale.ENGLISH) match {
      case "json" => Json
      case "soap" => Soap
      case _ => sys.error(s"Unknown connection type ${config.connectionType()}")
    }

    val url = new URI(config.chargeServerUrl())
    val charger = if (connectionType == Json) {
      new OcppJsonCharger(config.chargerId(), config.numberOfConnectors(), url)
    } else {
      val server = new ChargerServer(config.listenPort())
      new OcppSoapCharger(config.chargerId(), config.numberOfConnectors(), version, url, server)
    }

    (0 until config.numberOfConnectors()) map {
      c => system.actorOf(Props(new UserActor(charger.chargerActor, c, ActionIterator(config.passId()))))
    }
  }

  sealed trait ConnectionType
  case object Json extends ConnectionType
  case object Soap extends ConnectionType
}
