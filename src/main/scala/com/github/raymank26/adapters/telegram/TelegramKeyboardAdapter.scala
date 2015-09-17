package com.github.raymank26.adapters.telegram

import com.github.raymank26.controller.Telegram
import com.github.raymank26.controller.Telegram.Keyboard

import spray.json.{JsArray, JsBoolean, JsObject, JsString, JsValue, RootJsonWriter}

/**
 * [[Telegram.Keyboard]] serializer.
 *
 * @author Anton Ermak
 */
object TelegramKeyboardAdapter extends RootJsonWriter[Telegram.Keyboard] {
    override def write(obj: Keyboard): JsValue = {
        val fields = Map(
            "keyboard" -> convertButtons(obj.buttons),
            "resize_keyboard" -> JsBoolean(true),
            "one_time_keyboard" -> JsBoolean(obj.oneTimeKeyboard)
        )
        JsObject(fields)
    }

    private def convertButtons(buttons: Seq[Seq[String]]): JsArray = {
        buttons.foldLeft(JsArray()) { (jsArray, row) =>
            jsArray.copy(jsArray.elements :+ convertRow(row))
        }
    }

    private def convertRow(buttons: Seq[String]): JsArray = {
        buttons.foldLeft(JsArray()) { (jsArray, column) => jsArray.copy(
            jsArray.elements :+ JsString(column))
        }
    }
}

