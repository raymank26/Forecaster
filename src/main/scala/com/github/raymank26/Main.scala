package com.github.raymank26

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

        val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(routes, "localhost", 8090)

        println(s"Server online at http://localhost:8080/\n")
    }
}
