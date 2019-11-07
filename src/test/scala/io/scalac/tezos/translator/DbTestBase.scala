package io.scalac.tezos.translator

import io.scalac.tezos.translator.schema.TranslationTable
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcBackend
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait DbTestBase {

  def testDb: JdbcBackend#Database

  val dbTimeout: Duration = 5.seconds

  protected def run[R](dbAction: DBIOAction[R, NoStream, Nothing]): R = Await.result(testDb.run(dbAction), dbTimeout)

  protected def createTables(): Unit =
    run(
      DBIO.sequence(
        Seq(
          TranslationTable.translations.schema.create
        )
      )
    )

  protected def dropTables() = run {
    TranslationTable.translations.schema.dropIfExists
  }

  protected def recreateTables(): Unit = {
    dropTables()
    createTables()
  }

}
