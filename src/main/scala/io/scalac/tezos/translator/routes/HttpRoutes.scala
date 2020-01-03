package io.scalac.tezos.translator.routes

import akka.http.scaladsl.server.{Directives, Route}
import sttp.tapir.Endpoint

trait HttpRoutes extends Directives {

  def routes: Route

  def docs: List[Endpoint[_, _, _, _]]

}
