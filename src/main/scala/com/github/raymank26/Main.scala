package com.github.raymank26

import com.github.raymank26.db.Database
import com.github.raymank26.model.User

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer

import scala.concurrent.Future

/**
 * @author raymank26
 */
object Main extends Api {

    implicit val system = ActorSystem("forecast-system")
    implicit val materializer = ActorMaterializer()

    def main(args: Array[String]): Unit = {
        Database.saveUser(User(None, "Anton Ermak"))
        println(Database.getUsers)

        val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

        println(s"Server online at http://localhost:8080/\n")
    }
}
