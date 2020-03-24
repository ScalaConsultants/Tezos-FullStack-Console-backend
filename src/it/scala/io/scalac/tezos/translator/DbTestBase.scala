package io.scalac.tezos.translator

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.scalac.tezos.translator.config.DbEvolutionConfig
import io.scalac.tezos.translator.schema._
import slick.dbio.{ DBIOAction, NoStream }
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

object DbTestBase {

  lazy val db: PostgresProfile.backend.DatabaseDef =
    Database.forURL(container.jdbcUrl, container.username, container.password, executor = AsyncExecutor("exec", 10, 10, 1000, 10))
  val postgresVersion     = "postgres:9.6-alpine"
  val dbTimeout: Duration = 5.seconds
  val container           = new PostgreSQLContainer(Some(postgresVersion))
  container.start()
  val evolutionCfg = DbEvolutionConfig(container.jdbcUrl, container.username, container.password, "flyway", enabled = true)
  val dbEvolutions = SqlDbEvolution(evolutionCfg)

  def recreateTables()(implicit ec: ExecutionContext): Unit = {
    runDB(sqlu"DROP TABLE if exists flyway_schema_history;")
    dropTables()
    Await.result(dbEvolutions.runEvolutions().map(_ => ()), 20.seconds)
  }

  private def dropTables(): Seq[Unit] = runDB(
     DBIO.sequence(
        Seq(
           Emails2SendTable.emails2Send.schema.dropIfExists,
           LibraryTable.library.schema.dropIfExists,
           UsersTable.users.schema.dropIfExists
        )
     )
  )

  def runDB[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(db.run(dbAction), dbTimeout)

}
