package com.github.raymank26.db

import java.sql.Connection

import com.github.raymank26.db.HikariDb.DatabaseUrl

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class HikariDb(databaseUrl: DatabaseUrl) {

    val ds = new HikariDataSource(config)
    config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
    config.setJdbcUrl(databaseUrl.toString)
    private val config = new HikariConfig()

    def getConnection: Connection = ds.getConnection
}

object HikariDb {

    private lazy val instance = initDb()

    def apply: HikariDb = instance

    private def initDb(): HikariDb = {
        val config = ConfigFactory.load()
        new HikariDb(DatabaseUrl(
            config.getString("db.username"),
            config.getString("db.password"),
            config.getString("db.name")
        ))
    }

    case class DatabaseUrl(username: String, password: String, name: String) {
        override def toString: String = {
            s"jdbc:postgresql://localhost:5432/$name?user=$username&password=$password"
        }
    }

}
