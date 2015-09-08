package com.github.raymank26.actor

import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
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
                CommandProcessor() ! msg
            case _ => SettingsActor() ! msg
        }
        case msg => logger.error(s"no such handler for msg $msg")
    }
}

object MessageDispatcher {

    def apply()(implicit system: ActorSystem): ActorRef = system.actorOf(Props[MessageDispatcher])

}
