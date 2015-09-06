package com.github.raymank26

import com.github.raymank26.db.Database
import com.github.raymank26.model.User


/**
 * @author raymank26
 */
object Main {

    def main(args: Array[String]): Unit = {
        Database.saveUser(User(None, "Anton Ermak"))
        println(Database.getUsers)
    }
}
