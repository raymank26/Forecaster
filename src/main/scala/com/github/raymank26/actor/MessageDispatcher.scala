package com.github.raymank26.actor

import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.Actor
import akka.event.Logging

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class MessageDispatcher extends Actor {

    import context.system

    private val logger = Logging(context.system, this)

    override def receive: Receive = {
        case msg: TelegramMessage => msg.content match {
            case txt: TelegramMessage.Text if txt.text.startsWith("/") =>
                CommandProcessor.apply ! msg
            case _: TelegramMessage.Location => CommandProcessor.apply ! msg
        }
        case msg => logger.error(s"no such handler for msg $msg")
    }
}
