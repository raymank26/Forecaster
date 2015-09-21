package com.github.raymank26.actor

import com.github.raymank26.actor.CommandProcessor._
import com.github.raymank26.controller.Forecast.{Currently, Period, Today}
import com.github.raymank26.controller.{Forecast, Telegram}
import com.github.raymank26.db.Database
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.Text

import akka.actor.{Actor, ActorLogging, PoisonPill}

import scala.collection.immutable.HashMap

/**
 * This actor handles various input commands (starts with '/').
 *
 * @author Anton Ermak
 */
private final class CommandProcessor extends Actor with ActorLogging with Utils {

    import context.dispatcher

    private val commands: Map[String, TelegramMessage => Unit] = HashMap(
        CommandCurrent -> processForecast(Currently) _,
        CommandToday -> processForecast(Today) _,
        CommandHelp -> processHelp _,
        CommandSettings -> processSettings _,
        CommandStart -> processStart _
    )

    override def receive: Receive = {

        case msg: TelegramMessage if msg.content.isInstanceOf[Text] =>
            val command = msg.content.asInstanceOf[Text].text
            if (commands.contains(command)) {
                commands(command)(msg)
            } else {
                Telegram.sendMessage(s"Command isn't supported. Check $CommandHelp",
                    msg.from.chatId)
            }
            self ! PoisonPill
    }

    private def processStart(msg: TelegramMessage): Unit = {
        val from = sender()
        runAsFuture { () =>
            Telegram.sendMessage("Hi. I'm telegram bot", msg.from.chatId)
            from ! MessageDispatcher.WantSettings(msg.from.chatId)
        }
    }

    private def processSettings(msg: TelegramMessage): Unit = {
        sender() ! MessageDispatcher.WantSettings(msg.from.chatId)
    }

    private def processForecast(when: Period)(msg: TelegramMessage): Unit = {
        val from = msg.from
        val chatId = from.chatId

        runAsFuture { () =>
            Database.getPreferences(chatId) match {
                case Some(prefs) => Forecast.sendForecast(prefs, chatId, when)
                case None => preferencesRequired(chatId)
            }
        } onFailure {
            case _ => Telegram.sendErrorInfo(chatId)
        }
    }

    private def preferencesRequired(chatId: Int): Unit = {
        Telegram.sendMessage(s"I don't know you yet. Run $CommandSettings.", chatId)
    }

    private def processHelp(msg: TelegramMessage): Unit = {
        Telegram.sendMessage(HelpMessage, msg.from.chatId)
    }
}

private object CommandProcessor {

    val CommandCurrent = "/current"

    private val CommandHelp = "/help"
    private val CommandSettings = "/settings"
    private val CommandStart = "/start"
    private val CommandToday = "/today"

    private val HelpMessage =
    //@formatter:off
        s"""
          |I'm a forecast bot. The available commands are:
          |1. $CommandHelp - this message
          |2. $CommandCurrent - current forecast
          |3. $CommandToday - 12 messages of forward forecast
          |3. $CommandSettings - redefine settings
          |The author is @antonermak. Bot is powered by http://forecast.io and http://www.webcams.travel
        """.stripMargin
    //@formatter:on

}
