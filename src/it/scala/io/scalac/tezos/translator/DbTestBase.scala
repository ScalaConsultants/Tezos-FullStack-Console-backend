package io.scalac.tezos.translator

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.schema._
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object DbTestBase {
  val postgresVersion = "postgres:9.6-alpine"
  val dbTimeout: Duration = 5.seconds

  val container = new PostgreSQLContainer(Some(postgresVersion))
  container.start()

  lazy val db: PostgresProfile.backend.DatabaseDef =
    Database.forURL(
      container.jdbcUrl,
      container.username,
      container.password,
      executor = AsyncExecutor("exec", 10, 10, 1000, 10))

  def runDB[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(db.run(dbAction), dbTimeout)

  private def createTables()(implicit ec: ExecutionContext): Unit =
    runDB(
      DBIO.sequence(
        Seq(
          Emails2SendTable.emails2Send.schema.create,
          LibraryTable.library.schema.create,
          UsersTable.users.schema.create
        )
      ).flatMap( _ => UsersTable.users += UserModel("asdf", "$2a$10$Idx1kaM2XQbX72tRh9hFteQ5D5ooOnfO9pR/xYIcHQ/.5BrAnEyrW"))
    )

  private def dropTables(): Seq[Unit] = runDB(
    DBIO.sequence(
      Seq(
        Emails2SendTable.emails2Send.schema.dropIfExists,
        LibraryTable.library.schema.dropIfExists,
        UsersTable.users.schema.dropIfExists
      )
    )
  )

  def recreateTables()(implicit ec: ExecutionContext): Unit = {
    dropTables()
    createTables()
  }

}
