package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.repository.{Emails2SendRepository, TranslationRepository}
import io.scalac.tezos.translator.service.{Emails2SendService, TranslationsService}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import slick.jdbc.MySQLProfile.api._

object Boot {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("tezos-translator")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val log = system.log
    val config = ConfigFactory.load().getConfig("console")
    val httpConfig = config.getConfig("http")
    val host = httpConfig.getString("host")
    val port = httpConfig.getInt("port")
    val configuration = Configuration.getConfig(log)
    log.info(s"Config loaded - $configuration")

    implicit val db: MySQLProfile.backend.Database = Database.forConfig("tezos-db")
    implicit val repository: TranslationRepository = new TranslationRepository
    val emails2SendRepo = new Emails2SendRepository
    val email2SendService = new Emails2SendService(emails2SendRepo, db)

    val routes = new Routes(new TranslationsService, email2SendService, log, configuration)

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes.allRoutes, host, port)

    println(s"Server online at http://$host:$port\nPress RETURN to stop...")

    val _ = StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

  }
}
