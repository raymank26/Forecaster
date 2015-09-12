package com.github.raymank26

import com.github.raymank26.adapters.telegram.UpdateAdapter
import com.github.raymank26.model.telegram.TelegramMessage

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

    val routes = logResult(("foo", Logging.WarningLevel)) {
        (post & entity(as[(Int, TelegramMessage)])) { update => ctx =>

            processRequest(update._2)
            ctx.complete("OK")
        }
    }

    def processRequest(telegramMessage: TelegramMessage): Unit
}
