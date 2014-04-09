package com.thenewmotion.chargenetwork.ocpp.charger.json

import com.thenewmotion.ocpp.messages.{Req, Res}
import scala.concurrent.Future

trait OcppEndpoint[OUTREQ <: Req, INRES <: Res, INREQ <: Req, OUTRES <: Res] {
  def send(req: OUTREQ): Future[INRES]

  def onRequest(req: INREQ): Future[OUTRES]

  def onError(error: OcppError): Unit

  def onDisconnect: Unit
}
