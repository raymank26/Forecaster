package com.github.raymank26.actor

import com.github.raymank26.actor.CommandProcessor.HelpMessage
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.controller.{Forecast, Telegram, Webcams}
import com.github.raymank26.db.Database
import com.github.raymank26.model.forecast.DataPoint.IconType._
import com.github.raymank26.model.forecast.Weather
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.Text

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.event.{Logging, LoggingAdapter}

import scala.collection.immutable.HashMap
import scala.concurrent.Future

/**
 * @author Anton Ermak
 */
class CommandProcessor extends Actor with Utils {

    val logger: LoggingAdapter = Logging(context.system, this)

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

        val forecastFuture = runAsFuture(logger) {
            Database.getForecastPreferences(from)
        }
        forecastFuture.onSuccess {
            case Some(prefs) => sendForecast(sendTo, prefs, chatId)
            case None => preferencesRequired(sendTo, chatId)
        }
    }

    private def preferencesRequired(sender: ActorRef, chatId: Int) = runAsFuture(logger) {
        Telegram.sendMessage("Send to me your location firstly", chatId)
    }

    private def sendForecast(sender: ActorRef, prefs: GeoPrefs,
                             chatId: Int): Future[Unit] = runAsFuture(logger) {

        val forecast = Forecast.getCurrentForecast(prefs)
        val previews = Webcams.getLinks(prefs)
        Telegram.sendWebcamPreviews(previews, chatId)
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
    private val SnowSymbol = '\u2744'
    private val RainRymbol = '\u2614'

    def apply(context: ActorContext): ActorRef = context.actorOf(Props[CommandProcessor])

    private def makeForecastMessage(forecast: Weather): String = {
        val icon = serializeIcon(forecast.currently.icon)
        s"""
           | ${forecast.currently.summary} $icon.
                                                  |- temp is ${forecast.currently.temperature} °C;
                                                                                                |- apparent temp is ${
            forecast.currently.apparentTemperature
        } °C;
           |- wind speed ${forecast.currently.windSpeed} m/s.
         """.stripMargin
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

    case class TelegramResponse(value: String)

}
