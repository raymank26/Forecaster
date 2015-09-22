package com.github.raymank26.actor

import com.github.raymank26.actor.MessageSupervisor.CloseForwarding
import com.github.raymank26.controller.Telegram

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}

/**
 * Supervisor proxy. Sends message back to user in case of unhandled exceptions.
 *
 * @author Anton Ermak
 */
private final class MessageSupervisor(chatId: Int, props: Props) extends Actor {

    val actor = context.actorOf(props)

    override def receive: Actor.Receive = {
        case msg => actor forward msg
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        case e: Exception =>
            Telegram.sendMessage("Something went wrong. Try me later \uD83D\uDE22", chatId)
            context.parent ! CloseForwarding(chatId)
            Stop
        case _ => Escalate
    }
}

private object MessageSupervisor {

    def apply(chatId: Int, props: Props): Props = Props(classOf[MessageSupervisor], chatId, props)

    case class CloseForwarding(chatId: Int)

}


