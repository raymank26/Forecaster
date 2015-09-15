package com.github.raymank26.db

import com.github.raymank26.actor.SettingsFSM
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.model.telegram.TelegramUser

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak
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
    def getForecastPreferences(telegramUser: TelegramUser): Option[GeoPrefs] = {
        getUserDbId(telegramUser).flatMap { userId =>
            getForecastPreferences(userId).map(_._2)
        }
    }

    def saveSettings(data: SettingsFSM.Data) = ???

    /**
     * Saves forecast preferences
     *
     * @param telegramUser telegram user
     * @param forecastUserSettings settings to save
     */
    def saveOrUpdateForecastPreferences(telegramUser: TelegramUser,
                                        forecastUserSettings: GeoPrefs): Unit = {

        val userId = getUserOrSave(telegramUser)
        saveOrUpdateForecastPreferences(userId, forecastUserSettings)
    }

    private def getForecastPreferences(userId: Int): Option[(Int, GeoPrefs)] = {
        DB readOnly { implicit session =>
            sql"select id, latitude, longitude from $Preferences where user_id = ?"
                .bind(userId)
                .map(rs => mapRsToForecast(rs))
                .single()
                .apply()
        }
    }

    private def mapRsToForecast(rs: WrappedResultSet): (Int, GeoPrefs) = {
        (rs.int("id"), GeoPrefs(rs.double("latitude"), rs.double("longitude")))
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

    private def getUserOrSave(telegramUser: TelegramUser): Int = {
        getUserDbId(telegramUser).getOrElse(saveUser(telegramUser))
    }

    private def saveUser(user: TelegramUser): Int = {
        DB localTx { implicit session =>
            sql"""insert into $Users (username, user_id) values (${user.username }, ${user.chatId })"""
                .update(
                .apply()
        }
    }

    private def saveOrUpdateForecastPreferences(userId: Int,
                                                forecastUserSettings: GeoPrefs) = {
        getForecastPreferences(userId) match {
            case Some((id, _)) => updateForecastPreferences(id, forecastUserSettings)
            case None =>
                val latitude = forecastUserSettings.latitude
                val longitude = forecastUserSettings.longitude
                DB localTx { implicit session =>
                    //@formatter:off
                    sql"""insert into $Preferences(user_id, message_datetime, latitude, longitude)
                         |values ($userId, ${DateTime.now}, $latitude, $longitude)"""
                        .stripMargin
                        .update()
                        .apply()
                    //@formatter:on
                }
        }
    }

    private def updateForecastPreferences(rowId: Int,
                                          forecastUserSettings: GeoPrefs) = {
        DB localTx { implicit session =>
            sql"update $Preferences set latitude = ?, longitude = ? where id = ?"
                .bind(forecastUserSettings.latitude, forecastUserSettings.longitude, rowId)
                .update()
                .apply()
        }
    }
}
