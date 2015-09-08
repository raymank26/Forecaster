package com.github.raymank26.actor

import com.github.raymank26.controller.Forecast.ForecastUserSettings
import com.github.raymank26.controller.{Forecast, Telegram}
import com.github.raymank26.db.Database
import com.github.raymank26.model.forecast.Weather
import com.github.raymank26.model.telegram.TelegramMessage

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

import scala.concurrent.Future

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class CommandProcessor extends Actor {

    val logger = Logging(context.system, this)

    import context.dispatcher

    override def receive: Receive = {

        case TelegramMessage(_, from, _, TelegramMessage.Text(CommandProcessor.currentForecast)) =>

            val sendTo = sender()
            val chatId = from.chatId

            Future {
                Database.getForecastPreferences(from)
            } onSuccess {
                case Some(prefs) => sendForecast(sendTo, prefs, chatId)
                case None => preferencesRequired(sendTo, chatId)
            }

        case msg: TelegramMessage =>
            Telegram.sendMessage("Command isn't supported", msg.from.chatId)

        case cmd => logger.error(s"not such message supported $cmd")
    }

    private def getChatId(msg: TelegramMessage) = msg.from.chatId

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

    private val currentForecast = "current"

    def apply(implicit system: ActorSystem): ActorRef = system.actorOf(Props[CommandProcessor])

    private def makeForecastMessage(forecast: Weather): String = {
        s"""
           |The current forecast is ${forecast.currently.temperature}
         """.stripMargin
    }

    case class TelegramResponse(value: String)

}
