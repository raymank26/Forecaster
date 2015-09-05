package com.github.raymank26.db

import com.github.raymank26.model.User

import org.joda.time.DateTime
import scalikejdbc._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Database {

    private val Users = sqls"users"

    HikariDb.setSession()

    def getUsers: Seq[User] = {
        DB readOnly { implicit session =>
            sql"select * from $Users".map(rs => unwrapUser(rs)).list().apply
        }
    }

    def insertUser(user: User): Unit = {
        DB localTx { implicit session =>
            sql"""
                 |insert into $Users (name, message_datetime) values ( ?, ? );
               """.stripMargin.bind(user.name, user.messageDatetime).update().apply()
        }
    }

    def unwrapUser(rs: WrappedResultSet): User = {
        User(Some(rs.int("id")), rs.string("name"), DateTime.now)
    }
}
