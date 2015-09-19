package com.github.raymank26.adapter.telegram

import com.github.raymank26.adapter.forecast.DateTimeAdapter
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * [[TelegramMessage]] serializer.
 *
 * @author Anton Ermak
 */
object TelegramMessageAdapter extends RootJsonReader[TelegramMessage] {

    private val MemberLocation = "location"

    private implicit val telegramUserAdapter = TelegramUserAdapter
    private implicit val datetimeAdapter = DateTimeAdapter

    override def read(json: JsValue): TelegramMessage = {
        val jsonObject = json.asJsObject.fields
        TelegramMessage(id = jsonObject("message_id").convertTo[Int],
            from = jsonObject("from").convertTo[TelegramUser],
            date = jsonObject("date").convertTo[DateTime],
            content = getContent(jsonObject))
    }

    private def getContent(jsonObject: Map[String, JsValue]): TelegramMessage.Content = {
        if (jsonObject.isDefinedAt(MemberLocation)) {
            val location = jsonObject(MemberLocation).asJsObject.fields

            TelegramMessage.Location(
                location("latitude").convertTo[Double],
                location("longitude").convertTo[Double]
            )
        } else {
            TelegramMessage.Text(jsonObject("text").convertTo[String])
        }
    }
}
