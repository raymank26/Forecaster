package com.github.raymank26.actor

import com.github.raymank26.actor.CommandProcessor._
import com.github.raymank26.controller.{Forecast, Telegram, Webcams}
import com.github.raymank26.db.Database
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.forecast.DataPoint.IconType._
import com.github.raymank26.model.forecast.Weather
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.Text

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.immutable.HashMap
import scala.concurrent.Future

/**
 * @author Anton Ermak
 */
private final class CommandProcessor extends Actor with ActorLogging with Utils {

    import context.dispatcher

    private val commands: Map[String, TelegramMessage => Unit] = HashMap(
        CurrentCommand -> processCurrent _,
        HelpCommand -> processHelp _,
        SettingsCommand -> processSettings _,
        StartCommand -> processSettings _
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

    private def processSettings(msg: TelegramMessage): Unit = {
        sender() ! MessageDispatcher.WantSettings(msg.from.chatId)
    }

    private def processCurrent(msg: TelegramMessage): Unit = {
        val from = msg.from
        val sendTo = sender()
        val chatId = from.chatId

        val forecastFuture = runAsFuture {
            Database.getPreferences(from)
        }
        forecastFuture.onSuccess {
            case Some(prefs) => sendForecast(sendTo, prefs, chatId)
            case None => preferencesRequired(sendTo, chatId)
        }
    }

    private def preferencesRequired(sender: ActorRef, chatId: Int) = runAsFuture {
        Telegram.sendMessage("Send to me your location firstly", chatId)
    }

    private def sendForecast(sender: ActorRef, prefs: Preferences,
                             chatId: Int): Future[Unit] = runAsFuture {

        val forecast = Forecast.getCurrentForecast(prefs.geo, prefs.language)
        if (prefs.webcams.nonEmpty) {
            val previews = Webcams.getLinks(prefs.geo, prefs.webcams)
            Telegram.sendWebcamPreviews(previews, chatId)
        }
        Telegram.sendMessage(CommandProcessor.makeForecastMessage(forecast), chatId)
    }

    private def processHelp(msg: TelegramMessage): Unit = {
        Telegram.sendMessage(HelpMessage, msg.from.chatId)
    }
}

private object CommandProcessor {

    val CurrentCommand = "/current"

    private val HelpMessage =
        """
          |I'm a forecast bot. The available commands are:
          |1. /help - this help message
          |2. /current - current forecast
          |3. /settings - redefine settings
          |The author is @antonermak.
        """.stripMargin

    private val HelpCommand = "/help"
    private val SettingsCommand = "/settings"
    private val StartCommand = "/start"

    private val SnowSymbol = '\u2744'
    private val RainRymbol = '\u2614'

    private def makeForecastMessage(forecast: Weather): String = {
        val icon = serializeIcon(forecast.currently.icon)
        // @formatter:off
        s"""
           |${forecast.currently.summary} $icon
           |- temp is ${forecast.currently.temperature} °C;
           |- apparent temp is ${forecast.currently.apparentTemperature} °C;
           |- wind speed ${forecast.currently.windSpeed} m/s.
         """.stripMargin
        // @formatter: on
    }

    private def serializeIcon(icon: Icon): String = {

        icon match {

            case Rain => RainRymbol.toString

            case ClearDay => "\uD83C\uDF1D"

            case ClearNight => "\uD83C\uDF1A"

            case Cloudy => "\u2601"

            case Snow => SnowSymbol.toString

            case Sleet => (SnowSymbol + RainRymbol).toString

            case _ => ""
        }

    }
}
