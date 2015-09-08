package com.github.raymank26

import com.github.raymank26.adapters.telegram.TelegramMessageAdapter
import com.github.raymank26.model.telegram.TelegramMessage

import com.typesafe.scalalogging.Logger

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import org.slf4j.LoggerFactory

/**
 * @author Anton Ermak
 */
trait Api extends SprayJsonSupport {

    val logger = Logger(LoggerFactory.getLogger("Akka-Http-Routes"))

    implicit val telegramMessageAdapter = TelegramMessageAdapter

    implicit val materializer: Materializer
    implicit val system: ActorSystem

    val routes =
        path(s"/${ConfigManager.getBotId}/receiver") {
            (post & entity(as[TelegramMessage])) { msg => ctx =>
                processRequest(msg)
                ctx.complete("OK")
            }
        } ~ get { ctx =>
            logger.warn(s"No handler for ${ctx.request}")
            ctx.complete("FAIL")
        }

    def processRequest(telegramMessage: TelegramMessage): Unit
}
