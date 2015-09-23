package com.github.raymank26

import com.github.raymank26.actor.MessageDispatcher
import com.github.raymank26.db.{Database, FlywayManager}
import com.github.raymank26.model.telegram.TelegramMessage

import com.typesafe.scalalogging.Logger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.RejectionHandler
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory

/**
 * @author Anton Ermak
 */
object Main extends Api {

    implicit val system = ActorSystem("forecast-system")
    implicit val materializer = ActorMaterializer()

    private val logger = Logger(LoggerFactory.getLogger("main"))

    //@formatter:off
    implicit def rejectionHandler = RejectionHandler.newBuilder()
        .handle { case rej =>
            logger.warn(s"No handler found $rej")
            complete(ToResponseMarshallable.apply(OK))
    }.result()
    //@formatter:on

    def main(args: Array[String]): Unit = {
        FlywayManager.main(Array("migrate"))
        Database.init()

        val (host, port) = ConfigManager.getHostAndPort

        Http().bindAndHandle(routes, host, port)

        println(s"Server online at http://$host:$port/\n")
    }

    override def processRequest(telegramMessage: TelegramMessage): Unit =
        MessageDispatcher.getInstance() ! telegramMessage
}
