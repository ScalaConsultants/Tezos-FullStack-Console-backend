package io.scalac.tezos.translator

import io.scalac.tezos.translator.schema.{Emails2SendTable, TranslationTable}
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcBackend
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

trait DbTestBase {

  def testDb: JdbcBackend#Database

  val dbTimeout: Duration = 5.seconds

  protected def run[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(testDb.run(dbAction), dbTimeout)

  protected def createTables(): Unit =
    run(
      DBIO.sequence(
        Seq(
          TranslationTable.translations.schema.create,
          Emails2SendTable.emails2Send.schema.create
        )
      )
    )

  protected def dropTables(): Seq[Unit] = run(
    DBIO.sequence(
      Seq(
        TranslationTable.translations.schema.dropIfExists,
        Emails2SendTable.emails2Send.schema.dropIfExists
      )
    )
  )

  protected def recreateTables(): Unit = {
    dropTables()
    createTables()
  }

}
