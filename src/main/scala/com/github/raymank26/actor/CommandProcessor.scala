package com.github.raymank26.actor

import com.github.raymank26.actor.CommandProcessor.HelpMessage
import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.controller.{Forecast, Telegram}
import com.github.raymank26.db.Database
import com.github.raymank26.model.forecast.Weather
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.Text

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.event.Logging

import scala.collection.immutable.HashMap
import scala.concurrent.Future

/**
 * @author Anton Ermak
 */
class CommandProcessor extends Actor with Utils {

    val logger = Logging(context.system, this)

    import context.dispatcher

    val commands: Map[String, TelegramMessage => Unit] = HashMap(
        "/current" -> processCurrent _,
        "/help" -> processHelp _
    )

    override def receive: Receive = {

        case msg: TelegramMessage if msg.content.isInstanceOf[Text] =>
            val command = msg.content.asInstanceOf[Text].text
            if (commands.contains(command)) {
                commands(command)(msg)
            } else {
                Telegram.sendMessage("Command isn't supported", msg.from.chatId)
            }

        case msg => messageNotSupported(msg)
    }

    private def processCurrent(msg: TelegramMessage): Unit = {
        val from = msg.from
        val sendTo = sender()
        val chatId = from.chatId

        val forecastFuture = Future {
            Database.getForecastPreferences(from)
        }
        forecastFuture.onSuccess {
            case Some(prefs) => sendForecast(sendTo, prefs, chatId)
            case None => preferencesRequired(sendTo, chatId)
        }
        forecastFuture.onFailure {
            case exception: Throwable => logger.error(exception, "error")
        }
    }

    private def preferencesRequired(sender: ActorRef, chatId: Int) = Future {
        Telegram.sendMessage("Send to me your location firstly", chatId)
    }

    private def sendForecast(sender: ActorRef, prefs: ForecastUserSettings,
                             chatId: Int): Future[Unit] = Future {

        val forecast = Forecast.getCurrentForecast(prefs)
        Telegram.sendMessage(CommandProcessor.makeForecastMessage(forecast), chatId)
    }

    private def processHelp(msg: TelegramMessage): Unit = {
        Telegram.sendMessage(HelpMessage, msg.from.chatId)
    }
}

object CommandProcessor {
    private val HelpMessage =
        """
          |I'm a forecast bot. The available commands are:
          |1. /help - this help message
          |2. /current - current forecast
          |3. /settings - redefine settings
          |The author is @antonermak.
        """.stripMargin

    def apply(context: ActorContext): ActorRef = context.actorOf(Props[CommandProcessor])

    private def makeForecastMessage(forecast: Weather): String = {
        s"""
           |The current forecast is ${forecast.currently.temperature}
         """.stripMargin
    }

    case class TelegramResponse(value: String)

}
