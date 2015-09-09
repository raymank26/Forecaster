package com.github.raymank26.db

import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.model.telegram.TelegramUser

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Database {

    private val Users = sqls"users"
    private val Preferences = sqls"preferences"

    HikariDb.setSession()

    /**
     * Returns forecast preferences of [[TelegramUser]] instance if possible.
     *
     * @param telegramUser user instance
     * @return forecast preferences option
     */
    def getForecastPreferences(telegramUser: TelegramUser): Option[ForecastUserSettings] = {
        getUserDbId(telegramUser).flatMap { userId =>
            getForecastPreferences(userId).map(_._2)
        }
    }

    /**
     * Saves forecast preferences
     *
     * @param telegramUser telegram user
     * @param forecastUserSettings settings to save
     */
    def saveOrUpdateForecastPreferences(telegramUser: TelegramUser,
                                        forecastUserSettings: ForecastUserSettings): Unit = {

        val userId = getUserOrSave(telegramUser)
        saveOrUpdateForecastPreferences(userId, forecastUserSettings)
    }

    private def getUserOrSave(telegramUser: TelegramUser): Int = {
        getUserDbId(telegramUser).getOrElse(saveUser(telegramUser))
    }

    private def saveOrUpdateForecastPreferences(userId: Int,
                                                forecastUserSettings: ForecastUserSettings) = {
        getForecastPreferences(userId) match {
            case Some((id, _)) => updateForecastPreferences(id, forecastUserSettings)
            case None =>
                DB localTx { implicit session =>
                    sql"insert into $Preferences(user_id, message_datetime, latitude, longitude) values (?, ?, ?, ?)"
                        .bind(userId, DateTime.now, forecastUserSettings.latitude,
                            forecastUserSettings.longitude).update().apply()
                }
        }
    }

    private def updateForecastPreferences(rowId: Int,
                                          forecastUserSettings: ForecastUserSettings) = {
        DB localTx { implicit session =>
            sql"update $Preferences set latitude = ?, longitude = ? where id = ?"
                .bind(forecastUserSettings.latitude, forecastUserSettings.longitude, rowId)
                .update()
                .apply()
        }
    }

    private def getForecastPreferences(userId: Int): Option[(Int, ForecastUserSettings)] = {
        DB readOnly { implicit session =>
            sql"select id, latitude, longitude from $Preferences where user_id = ?"
                .bind(userId)
                .map(rs => mapRsToForecast(rs))
                .single()
                .apply()
        }
    }

    private def saveUser(user: TelegramUser): Int = {
        DB localTx { implicit session =>
            sql"""insert into $Users (username, user_id) values (?, ?)""".bind(user.username,
                user.chatId).update().apply()
        }
    }

    private def getUserDbId(user: TelegramUser): Option[Int] = {
        DB readOnly { implicit session =>
            sql"""select id from $Users where username = ?"""
                .bind(user.username)
                .map(rs => rs.int("id"))
                .single()
                .apply()
        }
    }

    private def mapRsToForecast(rs: WrappedResultSet): (Int, ForecastUserSettings) = {
        (rs.int("id"), ForecastUserSettings(rs.double("latitude"), rs.double("longitude")))
    }
}
