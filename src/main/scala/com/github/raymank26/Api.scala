package com.github.raymank26

import com.github.raymank26.adapter.telegram.UpdateAdapter
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUpdate}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LogEntry
import akka.stream.Materializer

/**
 * @author Anton Ermak
 */
trait Api extends SprayJsonSupport {

    lazy val requestLogger = (req: HttpRequest) => {
        val content = req.entity match {
            case Strict(_, data) => data.decodeString("UTF-8")
            case _ => ""
        }
        LogEntry((req.headers, content), Logging.InfoLevel)
    }

    implicit val materializer: Materializer
    implicit val system: ActorSystem
    implicit val updateAdapter = UpdateAdapter

    val routes = logRequest(requestLogger) {
        (post & entity(as[TelegramUpdate])) { update => ctx =>
            processRequest(update.telegramMessage)
            ctx.complete(StatusCodes.OK)
        }
    }

    def processRequest(telegramMessage: TelegramMessage): Unit
}
