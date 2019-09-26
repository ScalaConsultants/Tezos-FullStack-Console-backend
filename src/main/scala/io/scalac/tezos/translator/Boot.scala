package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.io.StdIn

object Boot {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val config = ConfigFactory.load().getConfig("console")
    val httpConfig = config.getConfig("http")
    val host = httpConfig.getString("host")
    val port = httpConfig.getInt("port")

    val routes = new Routes(new TranslationsService)

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes.allRoutes, host, port)

    println(s"Server online at http://$host:$port\nPress RETURN to stop...")

    val line = StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

  }
}
