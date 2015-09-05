package com.github.raymank26.adapters.telegram

import com.github.raymank26.adapters.forecast.DateTimeAdapter
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object TelegramMessageAdapter extends RootJsonReader[TelegramMessage] {

    implicit val telegramUserAdapter = TelegramUserAdapter
    implicit val datetimeAdapter = DateTimeAdapter

    override def read(json: JsValue): TelegramMessage = {
        val jsonObject = json.asJsObject.fields
        TelegramMessage(id = jsonObject("id").convertTo[Int],
            from = jsonObject("from").convertTo[TelegramUser],
            date = jsonObject("date").convertTo[DateTime],
            text = jsonObject("text").convertTo[String])
    }
}
