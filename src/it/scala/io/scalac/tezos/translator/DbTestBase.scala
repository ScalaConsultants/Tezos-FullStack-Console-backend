package io.scalac.tezos.translator

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.scalac.tezos.translator.model.UserModel
import io.scalac.tezos.translator.schema._
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{JdbcBackend, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


trait DbTestBase {

  def testDb: JdbcBackend#Database

  val dbTimeout: Duration = 5.seconds

  protected def runDB[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(testDb.run(dbAction), dbTimeout)

  protected def createTables()(implicit ec: ExecutionContext): Unit =
    runDB(
      DBIO.sequence(
        Seq(
          Emails2SendTable.emails2Send.schema.create,
          LibraryTable.library.schema.create,
          UsersTable.users.schema.create
        )
      ).flatMap( _ => UsersTable.users += UserModel("asdf", "$2a$10$Idx1kaM2XQbX72tRh9hFteQ5D5ooOnfO9pR/xYIcHQ/.5BrAnEyrW"))
    )

  protected def dropTables(): Seq[Unit] = runDB(
    DBIO.sequence(
      Seq(
        Emails2SendTable.emails2Send.schema.dropIfExists,
        LibraryTable.library.schema.dropIfExists
      )
    )
  )

  protected def recreateTables()(implicit ec: ExecutionContext): Unit = {
    dropTables()
    createTables()
  }

}

object DbTestBase {
  def dbFromContainer(container: PostgreSQLContainer): PostgresProfile.backend.DatabaseDef = {
    Database.forURL(
      container.jdbcUrl,
      container.username,
      container.password,
      executor = AsyncExecutor("exec", 10, 10, 1000, 10))
  }

  val postgresVersion = "postgres:9.6-alpine"
}
