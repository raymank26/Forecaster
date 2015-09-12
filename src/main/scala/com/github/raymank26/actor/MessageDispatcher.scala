package com.github.raymank26.actor

import com.github.raymank26.controller.Telegram
import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class MessageDispatcher extends Actor with Utils {

    import context.system

    private val logger = Logging(context.system, this)

    override def receive: Receive = {
        case msg: TelegramMessage => msg.content match {
            case txt: TelegramMessage.Text if txt.text.startsWith("/") =>
                CommandProcessor() ! msg
            case location: TelegramMessage.Location => SettingsActor() ! msg
            case _ => unsupportedMessage(msg)
        }
        case msg => messageNotSupported(msg)
    }

    private def unsupportedMessage(msg: TelegramMessage): Unit = {
        logger.warning(s"no such handler for $msg")
        Telegram.sendMessage("I don't understand you", msg.from.chatId)

    }
}

object MessageDispatcher {

    def apply()(implicit system: ActorSystem): ActorRef = system.actorOf(Props[MessageDispatcher])

}
