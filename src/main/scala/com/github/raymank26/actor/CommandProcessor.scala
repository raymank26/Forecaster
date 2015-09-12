package com.github.raymank26.actor

import com.github.raymank26.actor.CommandProcessor.currentForecast
import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.controller.{Forecast, Telegram}
import com.github.raymank26.db.Database
import com.github.raymank26.model.forecast.Weather
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.Text

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

import scala.concurrent.Future

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class CommandProcessor extends Actor with Utils {

    val logger = Logging(context.system, this)

    import context.dispatcher

    override def receive: Receive = {

        case msg: TelegramMessage if msg.content == Text(currentForecast) =>

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

        case msg: TelegramMessage =>
            Telegram.sendMessage("Command isn't supported", msg.from.chatId)

        case msg => messageNotSupported(msg)
    }

    private def preferencesRequired(sender: ActorRef, chatId: Int) = Future {
        Telegram.sendMessage("Send to me your location firstly", chatId)
    }

    private def sendForecast(sender: ActorRef, prefs: ForecastUserSettings,
                             chatId: Int): Future[Unit] = Future {

        val forecast = Forecast.getCurrentForecast(prefs)
        Telegram.sendMessage(CommandProcessor.makeForecastMessage(forecast), chatId)
    }
}

object CommandProcessor {

    private val currentForecast = "/current"

    def apply()(implicit system: ActorSystem): ActorRef = system.actorOf(Props[CommandProcessor])

    private def makeForecastMessage(forecast: Weather): String = {
        s"""
           |The current forecast is ${forecast.currently.temperature}
         """.stripMargin
    }

    case class TelegramResponse(value: String)

}
