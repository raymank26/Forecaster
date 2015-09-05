package com.github.raymank26

import com.github.raymank26.db.Database
import com.github.raymank26.model.User

import org.joda.time.DateTime


/**
 * @author raymank26
 */
object Main {

    def main(args: Array[String]): Unit = {
        Database.insertUser(User(None, "Anton Ermak", DateTime.now))
        println(Database.getUsers)
    }
}
