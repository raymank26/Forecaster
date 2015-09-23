package com.github.raymank26.db

import com.github.raymank26.model.Preferences
import com.github.raymank26.model.Preferences.Location
import com.github.raymank26.model.telegram.TelegramUser

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak
 */
object Database extends PreferencesProvider {

    private val UsersTableName = sqls"users"
    private val PreferencesTableName = sqls"preferences"

    def init(): Unit = {
        HikariDb.setSession()
    }

    override def getPreferences(chatId: Int): Option[Preferences] = {
        getUserDbId(chatId).flatMap { userId =>
            getPreferencesByDbId(userId).map(_._2)
        }
    }

    override def savePreferences(user: TelegramUser, prefs: Preferences) = {
        val userId = getUserOrSave(user)
        saveOrUpdatePreferences(userId, prefs)
    }

    /**
     * Deletes saved preferences and user itself.
     *
     * @param chatId user's identifier
     */
    def deleteData(chatId: Int): Unit = {
        //@formatter:off
        DB localTx { implicit session =>
            sql"""delete from $PreferencesTableName where user_id
                 |= (select id from $UsersTableName where chat_id = $chatId)"""
                .stripMargin
                .execute()
                .apply()

            sql"""delete from $UsersTableName where chat_id = $chatId"""
                .execute()
                .apply()
        }
        //@formatter:on
    }

    private def getPreferencesByDbId(userId: Int): Option[(Int, Preferences)] = {
        DB readOnly { implicit session =>
            //@formatter:off
            sql"""select id, latitude, longitude, webcams_ids
                 |from $PreferencesTableName where user_id = $userId"""
                .stripMargin
                .map(rs => mapRsToForecast(rs))
                .single()
                .apply()
            //@formatter:on
        }
    }

    private def mapRsToForecast(rs: WrappedResultSet): (Int, Preferences) = {
        val builder = new Preferences.Builder

        builder.setGeo(Location(rs.double("latitude"), rs.double("longitude")))

        rs.array("webcams_ids").getArray.asInstanceOf[Array[String]].foreach { item =>
            builder.addWebcam(item)
        }

        (rs.int("id"), builder.build())
    }

    private def getUserDbId(chatId: Int): Option[Int] = {
        DB readOnly { implicit session =>
            sql"""select id from $UsersTableName where chat_id = $chatId"""
                .map(rs => rs.int("id"))
                .single()
                .apply()
        }
    }

    private def getUserOrSave(telegramUser: TelegramUser): Int = {
        getUserDbId(telegramUser.chatId).getOrElse(saveUser(telegramUser))
    }

    private def saveUser(user: TelegramUser): Int = {
        DB localTx { implicit session =>
            //@formatter:off
            sql"""insert into $UsersTableName (first_name, username, chat_id)
                  |values (${user.firstName}, ${user.username}, ${user.chatId})
                """.stripMargin
                .updateAndReturnGeneratedKey()
                .apply()
                .toInt
            //@formatter:on
        }
    }

    private def saveOrUpdatePreferences(userId: Int,
                                        prefs: Preferences) = {
        getPreferencesByDbId(userId) match {
            case Some((id, _)) => updatePreferences(id, prefs)
            case None =>
                val latitude = prefs.geo.latitude
                val longitude = prefs.geo.longitude
                val webcams = prefs.webcams.toArray
                DB localTx { implicit session =>
                    //@formatter:off
                    sql"""insert into $PreferencesTableName
                         |(user_id, message_datetime, latitude, longitude, webcams_ids)
                         |values ($userId, ${DateTime.now}, $latitude, $longitude,
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
                  |webcams_ids = ${session.connection.createArrayOf("varchar", prefs.webcams.toArray)}
                  |where id = $rowId"""
                .stripMargin
                .update()
                .apply()
            //@formatter:on
        }
    }
}
