package com.github.raymank26.actor

import com.github.raymank26.actor.MessageSupervisor.CloseForwarding
import com.github.raymank26.controller.Telegram
import com.github.raymank26.model.telegram.TelegramUser

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}

/**
 * Supervisor proxy. Sends message back to user in case of unhandled exceptions.
 *
 * @author Anton Ermak
 */
private final class MessageSupervisor(telegramUser: TelegramUser, props: Props) extends Actor {

    val actor = context.actorOf(props)

    override def receive: Actor.Receive = {
        case msg => actor forward msg
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        case e: Exception =>
            Telegram.sendMessage("Something went wrong. Try me later \uD83D\uDE22",
                telegramUser.chatId)
            context.parent ! CloseForwarding(telegramUser)
            Stop
        case _ => Escalate
    }
}

private object MessageSupervisor {

    def apply(user: TelegramUser, props: Props): Props =
        Props(classOf[MessageSupervisor], user, props)

    case class CloseForwarding(telegramUser: TelegramUser)

}


