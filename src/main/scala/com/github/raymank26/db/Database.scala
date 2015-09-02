package com.github.raymank26.db

import com.github.raymank26.model.{Preferences, User}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Database {

    def getUser: Option[User] = {
        val connection = HikariDb.getConnection
        ???
    }

    def getUserPreferences(user: User): Preferences = ???
}
