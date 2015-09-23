package com.github.raymank26.adapter.telegram

import com.github.raymank26.model.telegram.TelegramUser

import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * [[TelegramUser]] serializer.
 *
 * @author Anton Ermak
 */
object TelegramUserAdapter extends RootJsonReader[TelegramUser] {
    override def read(json: JsValue): TelegramUser = {
        TelegramUser(
            firstName = json.asJsObject.fields("first_name").convertTo[String],
            username = json.asJsObject.fields.get("username").map(_.convertTo[String]),
            chatId = json.asJsObject.fields("id").convertTo[Int]
        )
    }
}
