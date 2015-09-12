package com.github.raymank26

import com.github.raymank26.adapters.telegram.UpdateAdapter
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUpdate}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

/**
 * @author Anton Ermak
 */
trait Api extends SprayJsonSupport {

    implicit val materializer: Materializer
    implicit val system: ActorSystem

    implicit val updateAdapter = UpdateAdapter

    val routes = logResult(("routes", Logging.WarningLevel)) {
        (post & entity(as[TelegramUpdate])) { update => ctx =>
            processRequest(update.telegramMessage)
            ctx.complete("OK")
        }
    }

    def processRequest(telegramMessage: TelegramMessage): Unit
}
