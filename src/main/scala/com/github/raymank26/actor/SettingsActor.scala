package com.github.raymank26.actor

import com.github.raymank26.controller.Telegram
import com.github.raymank26.db.Database
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class SettingsActor extends Actor {

    import context.dispatcher

    override def receive: Receive = {

        case msg: TelegramMessage => msg.content match {

            case location: TelegramMessage.Location =>
                saveLocation(location, msg.from) onComplete {
                    case Success(_) => Telegram.sendMessage("location saved", msg.from.chatId)
                    case Failure(_) => Telegram.sendMessage("something went wrong", msg.from.chatId)
                }

            case _ => Telegram.sendMessage("location?", msg.from.chatId)
        }
    }

    private def saveLocation(location: TelegramMessage.Location, user: TelegramUser) = {
        Future {
            Database.saveLocation(user, location)
        }
    }
}

object SettingsActor {

    def apply()(implicit system: ActorSystem): ActorRef = {
        system.actorOf(Props[SettingsActor])
    }

}
