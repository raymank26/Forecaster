package com.github.raymank26.actor

import com.github.raymank26.controller.Telegram
import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.routing.RoundRobinPool

/**
 * @author Anton Ermak
 */
class MessageDispatcher extends Actor with Utils {

    private val logger = Logging(context.system, this)

    private val commandRouter = context.actorOf(RoundRobinPool(5).props(Props[CommandProcessor]),
        "command-router")
    private val settingsRouter = context.actorOf(RoundRobinPool(5).props(Props[SettingsActor]),
        "settings-router")

    override def receive: Receive = {
        case msg: TelegramMessage => msg.content match {
            case txt: TelegramMessage.Text if txt.text.startsWith("/") => commandRouter ! msg
            case location: TelegramMessage.Location => settingsRouter ! msg
            case _ => unsupportedMessage(msg)
        }
        case msg => messageNotSupported(msg)
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        case _: Exception => Stop
    }

    private def unsupportedMessage(msg: TelegramMessage): Unit = {
        logger.warning(s"no such handler for $msg")
        Telegram.sendMessage("I don't understand you", msg.from.chatId)

    }
}

object MessageDispatcher {

    def apply()(implicit system: ActorSystem): ActorRef = system.actorOf(Props[MessageDispatcher])

}
