package com.github.raymank26.db

import org.flywaydb.core.Flyway

/**
 * @author Anton Ermak
 */
object FlywayManager {

    private val flyway = new Flyway()

    flyway.setDataSource(HikariDb.getDataSource)

    def main(args: Array[String]): Unit = {
        args(0) match {
            case "migrate" => flyway.migrate()
            case "repair" => flyway.repair()
            case "clean" => flyway.clean()
            case "info" => flyway.info()
            case "baseline" => flyway.baseline()
        }
    }
}
