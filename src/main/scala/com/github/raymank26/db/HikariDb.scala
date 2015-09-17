package com.github.raymank26.db

import java.sql.Connection
import java.util.Properties
import javax.sql.DataSource

import com.github.raymank26.db.HikariDb.DatabaseUrl

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

/**
 * @author Anton Ermak
 */
private class HikariDb(databaseUrl: DatabaseUrl) {

    private val dataSource: DataSource = {
        val config = new HikariConfig()
        val url = s"jdbc:postgresql://${databaseUrl.host}:${databaseUrl.port}/${databaseUrl.name}"
        config.setJdbcUrl(url)
        config.setDataSourceProperties(getDbProperties)
        new HikariDataSource(config)
    }

    def getConnection: Connection = dataSource.getConnection

    private def getDbProperties: Properties = {
        val prop = new Properties
        prop.setProperty("user", databaseUrl.username)
        prop.setProperty("password", databaseUrl.password)
        prop
    }
}

private object HikariDb {

    private lazy val instance = initDb()

    def getConnection: Connection = instance.getConnection

    def setSession(): Unit = {
        ConnectionPool.singleton(new DataSourceConnectionPool(getDataSource))
    }

    def getDataSource: DataSource = instance.dataSource

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

    case class DatabaseUrl(host: String, name: String, password: String,
                           username: String, port: Int)

}
