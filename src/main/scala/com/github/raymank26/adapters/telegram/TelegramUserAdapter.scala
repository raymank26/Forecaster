package com.github.raymank26.adapters.telegram

import com.github.raymank26.model.telegram.TelegramUser

import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object TelegramUserAdapter extends RootJsonReader[TelegramUser] {
    override def read(json: JsValue): TelegramUser = {
        TelegramUser(json.asJsObject.fields("username").convertTo[String])
    }
}
