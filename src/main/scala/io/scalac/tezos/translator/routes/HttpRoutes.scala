package io.scalac.tezos.translator.routes

import akka.http.scaladsl.server.{Directives, Route}

trait HttpRoutes extends Directives {

  def routes: Route
}
