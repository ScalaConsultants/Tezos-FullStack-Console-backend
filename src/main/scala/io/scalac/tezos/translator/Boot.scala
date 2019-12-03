package io.scalac.tezos.translator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import io.scalac.tezos.translator.actor.EmailSender
import io.scalac.tezos.translator.config.Configuration
import io.scalac.tezos.translator.repository.{Emails2SendRepository, LibraryRepository, UserRepository}
import io.scalac.tezos.translator.routes.util.MMTranslator
import io.scalac.tezos.translator.service.{Emails2SendService, LibraryService, UserService}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

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

    implicit val db: PostgresProfile.backend.Database = Database.forConfig("tezos-db")
    val emails2SendRepo = new Emails2SendRepository
    val libraryRepo     = new LibraryRepository(configuration.dbUtility)
    val userRepository = new UserRepository
    val email2SendService = new Emails2SendService(emails2SendRepo, db)
    val libraryService    = new LibraryService(libraryRepo, db)
    val userService = new UserService(userRepository, db)
    val cronEmailSender = EmailSender(email2SendService, configuration, log)

    val routes = new Routes(email2SendService, libraryService, userService, MMTranslator, log, configuration)

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes.allRoutes, host, port)

    log.info(s"Server online at http://$host:$port\nPress RETURN to stop...")

    StdIn.readLine()
    bindingFuture
      .flatMap { binding =>
        cronEmailSender.cancel()
        binding.unbind()
      }
      .onComplete(_ => system.terminate())

  }
}
