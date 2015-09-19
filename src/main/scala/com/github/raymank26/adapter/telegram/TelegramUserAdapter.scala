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
            username = json.asJsObject.fields("username").convertTo[String],
            chatId = json.asJsObject.fields("id").convertTo[Int]
        )

    }
}
