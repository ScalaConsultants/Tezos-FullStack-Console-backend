package io.scalac.tezos.translator

import com.dimafeng.testcontainers.MySQLContainer
import io.scalac.tezos.translator.schema._
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{JdbcBackend, MySQLProfile}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DbTestBase {

  def testDb: JdbcBackend#Database

  val dbTimeout: Duration = 5.seconds

  protected def runDB[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(testDb.run(dbAction), dbTimeout)

  protected def createTables(): Unit =
    runDB(
      DBIO.sequence(
        Seq(
          Emails2SendTable.emails2Send.schema.create,
          LibraryTable.library.schema.create,
          UsersTable.users.schema.create
        )
      )
    )

  protected def dropTables(): Seq[Unit] = runDB(
    DBIO.sequence(
      Seq(
        Emails2SendTable.emails2Send.schema.dropIfExists,
        LibraryTable.library.schema.dropIfExists
      )
    )
  )

  protected def recreateTables(): Unit = {
    dropTables()
    createTables()
  }

}

object DbTestBase {
  def dbFromContainer(container: MySQLContainer): MySQLProfile.backend.DatabaseDef = {
    Database.forURL(
      container.jdbcUrl,
      container.username,
      container.password,
      executor = AsyncExecutor("exec", 10, 10, 1000, 10))
  }

  val mySqlVersion = "mysql:8.0.18"
}
