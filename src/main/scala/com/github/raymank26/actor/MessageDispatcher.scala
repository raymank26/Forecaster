package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.{SettingsSaved, WantSettings}
import com.github.raymank26.controller.Telegram
import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.routing.RoundRobinPool

import scala.collection.mutable

/**
 * @author Anton Ermak
 */
private final class MessageDispatcher extends Actor with Utils {

    private val logger = Logging(context.system, this)

    private val commandRouter = context.actorOf(RoundRobinPool(5).props(Props[CommandProcessor]),
        "command-router")

    private val inSettings: mutable.Map[Int, ActorRef] = collection.mutable.Map.empty[Int, ActorRef]

    override def receive: Receive = {

        case msg: TelegramMessage if inSettings.contains(msg.from.chatId) =>
            inSettings(msg.from.chatId) ! msg

        case SettingsSaved(chatId) =>
            inSettings.remove(chatId).get

        case msg: TelegramMessage => msg.content match {
            case txt: TelegramMessage.Text if txt.text.startsWith("/") => commandRouter ! msg
            case _ => unsupportedMessage(msg)
        }

        case WantSettings(chatId) =>
            inSettings(chatId) = SettingsFSM(chatId, self, context)

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

    private var instance: Option[ActorRef] = None

    def getInstance()(implicit system: ActorSystem): ActorRef =
        instance match {
            case Some(ref) => ref
            case None =>
                instance = Some(system.actorOf(Props[MessageDispatcher]))
                instance.get
        }

    private[actor] case class WantSettings(chatId: Int)

    private[actor] case class SettingsSaved(chatId: Int)
}
