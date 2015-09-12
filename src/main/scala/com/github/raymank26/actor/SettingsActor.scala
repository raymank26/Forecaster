package com.github.raymank26.actor

import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.controller.Telegram
import com.github.raymank26.db.Database
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.event.Logging

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * @author Anton Ermak
 */
class SettingsActor extends Actor with Utils {

    import context.dispatcher

    private val logger = Logging(context.system, this)

    override def receive: Receive = {

        case msg: TelegramMessage => msg.content match {

            case location: TelegramMessage.Location =>
                saveLocation(location, msg.from) onComplete {
                    case Success(_) => Telegram.sendMessage("Location saved", msg.from.chatId)
                    case Failure(ex) =>
                        logger.error(ex, "location save error")
                        Telegram.sendMessage("Something went wrong", msg.from.chatId)
                }

            case _ => messageNotSupported(msg)
        }
    }

    private def saveLocation(location: TelegramMessage.Location, user: TelegramUser) = {
        Future {
            Database.saveOrUpdateForecastPreferences(user,
                ForecastUserSettings(location.latitude, location.longitude))
        }
    }
}

object SettingsActor {

    def apply(context: ActorContext): ActorRef = {
        context.actorOf(Props[SettingsActor])
    }

}
