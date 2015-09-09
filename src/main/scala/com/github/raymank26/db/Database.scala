package com.github.raymank26.db

import com.github.raymank26.controller.Forecast
import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.{Preferences, User}

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Database {

    private val Users = sqls"users"
    private val Preferences = sqls"preferences"

    HikariDb.setSession()

    def saveLocation(telegramUser: TelegramUser, location: TelegramMessage.Location): Unit = {
        val userId: Option[Int] = DB readOnly { implicit session =>
            sql"""select user_id from $Users where username = ?"""
                .bind(telegramUser.username)
                .map(rs => rs.int(0))
                .single()
                .apply()

        }
        DB localTx { implicit session =>
            sql"""insert into $Preferences (message_datetime, user_id, latitude, longitude) values (?, ?, ?, ?)"""
                .stripMargin
                .bind(DateTime.now(), userId, location.latitude, location.longitude)
                .update()
                .apply()
        }
    }

    /**
     * Returns forecast preferences of [[TelegramUser]] instance if possible.
     *
     * @param telegramUser user instance
     * @return forecast preferences option
     */
    def getForecastPreferences(telegramUser: TelegramUser): Option[ForecastUserSettings] = {
        getUserDbId(telegramUser).flatMap { userId =>
            DB readOnly { implicit session =>
                sql"select (latitude, longitude) from $Preferences where user_id = ?"
                    .bind(userId)
                    .map(rs => Forecast.ForecastUserSettings(rs.double(0), rs.double(1)))
                    .single()
                    .apply()
            }
        }
    }

    def createUserPreferences(preferences: Preferences, userId: Int) = {
        DB localTx { implicit session =>
            sql"""insert into $Preferences (message_datetime, user_id, latitude, longitude) values (?, ?, ?, ?)"""
                .stripMargin
                .bind(preferences.forecastReportDatetime, userId, preferences.latitude,
                    preferences.longitude)
        }
    }

    def saveUser(user: TelegramUser): Unit = {
        DB localTx { implicit session =>
            sql"""insert into $Users (username, user_id) values (?, ?)""".bind(user.username,
                user.chatId).update().apply()
        }
    }

    def getUserDbId(user: TelegramUser): Option[Int] = {
        DB readOnly { implicit session =>
            sql"""select id from $Users where username = ?"""
                .bind(user.username)
                .map(rs => rs.int(0))
                .single()
                .apply()
        }
    }

    private def unwrapUser(rs: WrappedResultSet): User = {
        User(Some(rs.int("id")), rs.string("name"))
    }

    private def unwrapPreferences(rs: WrappedResultSet): Preferences = ???
}
