package com.github.raymank26

import com.github.raymank26.actor.MessageDispatcher
import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer

import scala.concurrent.Future

/**
 * @author Anton Ermak
 */
object Main extends Api {

    implicit val system = ActorSystem("forecast-system")
    implicit val materializer = ActorMaterializer()

    def main(args: Array[String]): Unit = {

        val (host, port) = ConfigManager.getHostAndPort

        val bindingFuture: Future[ServerBinding] = Http().bindAndHandle(routes, host, port)

        println(s"Server online at http://$host:$port/\n")
    }

    override def processRequest(telegramMessage: TelegramMessage): Unit =
        MessageDispatcher.getInstance() ! telegramMessage
}
