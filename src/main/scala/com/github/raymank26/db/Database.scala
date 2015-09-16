package com.github.raymank26.db

import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.telegram.TelegramUser

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak
 */
object Database extends PreferencesProvider {

    private val Users = sqls"users"
    private val PreferencesTableName = sqls"preferences"

    HikariDb.setSession()

    override def getPreferences(telegramUser: TelegramUser): Option[Preferences] = {
        getUserDbId(telegramUser).flatMap { userId =>
            getPreferences(userId).map(_._2)
        }
    }

    override def savePreferences(user: TelegramUser, prefs: Preferences) = {
        val userId = getUserOrSave(user)
        saveOrUpdatePreferences(userId, prefs)
    }

    private def getPreferences(userId: Int): Option[(Int, Preferences)] = {
        DB readOnly { implicit session =>
            //@formatter:off
            sql"""select id, latitude, longitude, language, webcams_ids
                 |from $PreferencesTableName where user_id = ?""".stripMargin
                .bind(userId)
                .map(rs => mapRsToForecast(rs))
                .single()
                .apply()
            //@formatter:on
        }
    }

    private def mapRsToForecast(rs: WrappedResultSet): (Int, Preferences) = {
        val builder = new Preferences.Builder

        builder.setLanguage(rs.string("language"))
        builder.setGeo(GeoPrefs(rs.double("latitude"), rs.double("longitude")))

        rs.array("webcams_ids").getArray.asInstanceOf[Array[String]].foreach { item =>
            builder.addWebcam(item)
        }

        (rs.int("id"), builder.build())
    }

    private def getUserDbId(user: TelegramUser): Option[Int] = {
        DB readOnly { implicit session =>
            sql"""select id from $Users where user_id = ?"""
                .bind(user.chatId)
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
            //@formatter:off
            sql"""insert into $Users (username, user_id)
                 |values (${user.username }, ${user.chatId})""".stripMargin
                .update()
                .apply()
            //@formatter:on
        }
    }

    private def saveOrUpdatePreferences(userId: Int,
                                        prefs: Preferences) = {
        getPreferences(userId) match {
            case Some((id, _)) => updatePreferences(id, prefs)
            case None =>
                val latitude = prefs.geo.latitude
                val longitude = prefs.geo.longitude
                val language = prefs.language
                val webcams = prefs.webcams.toArray
                DB localTx { implicit session =>
                    //@formatter:off
                    sql"""insert into $PreferencesTableName
                         |(user_id, message_datetime, latitude, longitude, language, webcams_ids)
                         |values ($userId, ${DateTime.now}, $latitude, $longitude, $language::lang,
                         |${session.connection.createArrayOf("varchar", webcams.toArray)})"""
                        .stripMargin
                        .update()
                        .apply()
                    //@formatter:on
                }
        }
    }

    private def updatePreferences(rowId: Int,
                                  prefs: Preferences) = {
        DB localTx { implicit session =>
            //@formatter:off
            sql"""|update $PreferencesTableName set
                  |latitude = ${prefs.geo.latitude},
                  |longitude = ${prefs.geo.longitude},
                  |language = ${prefs.language}::lang,
                  |webcams_ids = ${session.connection.createArrayOf("varchar", prefs.webcams.toArray)}
                  |where id = $rowId"""
                .stripMargin
                .update()
                .apply()
            //@formatter:on
        }
    }
}
