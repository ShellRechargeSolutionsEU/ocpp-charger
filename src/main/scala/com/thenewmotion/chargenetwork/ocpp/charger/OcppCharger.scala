package com.thenewmotion.chargenetwork.ocpp.charger

import akka.actor.{Props, ActorSystem}
import com.thenewmotion.ocpp.{Version, CentralSystemClient}
import java.net.URI
import dispatch._
import org.rogach.scallop._

/**
 * @author Yaroslav Klymko
 */
object OcppCharger {
  def main(args: Array[String]) {
    object config extends ScallopConf(args) {
      val chargerId = opt[String]("id", descr = "Charge point ID of emulated charge point", default = Some("00055103978E"))
      val numberOfConnectors = opt[Int]("connectors", descr = "Number of connectors of emulated charge point", default = Some(2))
      val protocolVersion = opt[String]("protocol-version", descr = "OCPP version (either \"1.2\" or \"1.5\"", default = Some("1.5"))
      val chargeServerUrl = trailArg[String](descr = "Charge server URL base (without trailing slash)", default = Some("http://127.0.0.1:8081"))
    }

    implicit val system = ActorSystem("ocpp-simulator")
    val version = try {
      Version.withName(config.protocolVersion())
    } catch {
      case e: NoSuchElementException => sys.error("Unknown protocol version " + config.protocolVersion())
    }

    val url = new URI(config.chargeServerUrl() + "/ocpp/")
    val client = CentralSystemClient(config.chargerId(), version, url, new Http)

    val charger = system.actorOf(Props(new ChargerActor(BosService(config.chargerId(), client), config.numberOfConnectors())))

    (0 until config.numberOfConnectors()) map {
      c => system.actorOf(Props(new UserActor(charger, c, ActionIterator())))
    }
  }
}
