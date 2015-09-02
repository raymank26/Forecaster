package com.github.raymank26

import com.github.raymank26.db.HikariDb

/**
 * @author raymank26
 */
object Main {

    def main(args: Array[String]) {
        val connection = HikariDb.getConnection
        println(connection)
    }
}
