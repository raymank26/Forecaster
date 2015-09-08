package com.github.raymank26.db

import com.github.raymank26.controller.Forecast
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.{Preferences, User}

import scalikejdbc._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Database {

    private val Users = sqls"users"
    private val Preferences = sqls"preferences"

    HikariDb.setSession()

    def getUsers: Seq[User] = {
        DB readOnly { implicit session =>
            sql"select * from $Users".map(rs => unwrapUser(rs)).list().apply
        }
    }

    def saveUser(user: User): Unit = {
        DB localTx { implicit session =>
            sql"insert into $Users (name) values (?);"
                .bind(user.name).update().apply()
        }
    }

    def getUserPreferences(username: String): Option[Preferences] = {
        DB readOnly { implicit session =>
            sql"select * from $Preferences where user_id = ?"
                .bind(username)
                .map(rs => unwrapPreferences(rs))
                .single()
                .apply()
        }
    }

    def saveLocation(telegramUser: TelegramUser, location: TelegramMessage.Location) = {
        ???
    }

    def getForecastPreferences(telegramUser: TelegramUser): Option[Forecast.ForecastUserSettings]
    = {
        ???
    }

    def createUserPreferences(preferences: Preferences, userId: Int) = {
        DB localTx { implicit session =>
            sql"""insert into $Preferences (message_datetime, user_id, latitude, longitude)
                                            |values (?, ?, ?, ?)
               """
                .stripMargin
                .bind(preferences.forecastReportDatetime, userId, preferences.latitude,
                    preferences.longitude)
        }
    }

    private def unwrapUser(rs: WrappedResultSet): User = {
        User(Some(rs.int("id")), rs.string("name"))
    }

    private def unwrapPreferences(rs: WrappedResultSet): Preferences = ???
}
