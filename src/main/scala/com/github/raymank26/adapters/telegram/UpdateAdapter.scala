package com.github.raymank26.adapters.telegram

import com.github.raymank26.model.telegram.TelegramUpdate

import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object UpdateAdapter extends RootJsonReader[TelegramUpdate] {

    private val MemberUpdateId = "update_id"
    private val MemberMessage = "message"

    override def read(json: JsValue): TelegramUpdate = {
        val jsonObject = json.asJsObject.fields
        TelegramUpdate(jsonObject(MemberUpdateId).convertTo[Int],
            TelegramMessageAdapter.read(jsonObject(MemberMessage)))
    }
}
