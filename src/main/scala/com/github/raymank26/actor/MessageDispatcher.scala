package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.{SettingsSaved, WantSettings}
import com.github.raymank26.controller.Telegram
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.collection.mutable

/**
 * Main actor which handles all incoming messages and passes them to appropriate actors.
 *
 * @author Anton Ermak
 */
private final class MessageDispatcher extends Actor with ActorLogging with Utils {

    private val inSettings: mutable.Map[TelegramUser, ActorRef] =
        mutable.Map.empty[TelegramUser, ActorRef]

    override def receive: Receive = {

        case msg: TelegramMessage if inSettings.contains(msg.from) =>
            inSettings(msg.from) ! msg

        case SettingsSaved(user) =>
            inSettings.remove(user)

        case msg: TelegramMessage => msg.content match {
            case txt: TelegramMessage.Text if txt.text.startsWith("/") =>
                context.actorOf(MessageSupervisor.apply(msg.from, Props[CommandProcessor])) ! msg
            case _ => unsupportedMessage(msg)
        }

        case WantSettings(user) =>
            inSettings(user) = context.actorOf(MessageSupervisor.apply(user,
                SettingsFSM(user, self)))

        case MessageSupervisor.CloseForwarding(telegramUser) =>
            inSettings.remove(telegramUser)

        case msg => messageIsNotSupported(msg)
    }

    private def unsupportedMessage(msg: TelegramMessage): Unit = {
        log.warning(s"no such handler for $msg")
        Telegram.sendNotUnderstand(msg.from)
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

    private[actor] case class WantSettings(telegramUser: TelegramUser)

    private[actor] case class SettingsSaved(telegramUser: TelegramUser)

}
