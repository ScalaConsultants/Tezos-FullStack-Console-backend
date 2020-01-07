package io.scalac.tezos.translator

import io.scalac.tezos.translator.config.DbEvolutionConfig
import org.flywaydb.core.Flyway

import scala.concurrent.{ ExecutionContext, Future }

class SqlDbEvolution(flyway: Flyway) {

  /**
   * @return Migrations count
   */
  def runEvolutions()(implicit ec: ExecutionContext): Future[Int] = Future(flyway.migrate())

}

object SqlDbEvolution {

  def apply(config: DbEvolutionConfig, additionalMigrationPackages: List[String] = List.empty): SqlDbEvolution = {
    lazy val flyway: Flyway = Flyway
      .configure()
      .dataSource(config.url, config.user, config.password)
      .locations(config.migrationScriptsPackage :: additionalMigrationPackages: _*)
      .load()

    new SqlDbEvolution(flyway)
  }
}
