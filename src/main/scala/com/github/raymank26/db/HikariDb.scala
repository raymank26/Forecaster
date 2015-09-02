package com.github.raymank26.db

import java.sql.Connection
import java.util.Properties

import com.github.raymank26.db.HikariDb.DatabaseUrl

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class HikariDb(databaseUrl: DatabaseUrl) {

    private val config = new HikariConfig()
    config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
    config.setDataSourceProperties(getDbProperties)

    val ds = new HikariDataSource(config)

    def getConnection: Connection = ds.getConnection

    private def getDbProperties: Properties = {
        val prop = new Properties
        prop.setProperty("user", databaseUrl.username)
        prop.setProperty("password", databaseUrl.password)
        prop.setProperty("serverName", databaseUrl.host)
        prop.setProperty("portNumber", databaseUrl.port.toString)
        prop
    }
}

object HikariDb {

    private lazy val instance = initDb()

    def getConnection: Connection = instance.getConnection

    private def initDb(): HikariDb = {
        val config = ConfigFactory.load()
        new HikariDb(DatabaseUrl(
            config.getString("forecaster.db.host"),
            config.getString("forecaster.db.name"),
            config.getString("forecaster.db.password"),
            config.getString("forecaster.db.username"),
            config.getInt("forecaster.db.port")
        ))
    }

    case class DatabaseUrl(host: String, name: String, password: String, username: String,
                           port: Int)
}
