package com.github.raymank26.controller

import com.github.raymank26.ConfigManager
import com.github.raymank26.adapter.telegram.TelegramKeyboardAdapter
import com.github.raymank26.model.webcam.WebcamPreviewList

import spray.json._

import scalaj.http.{Http, MultiPart}

/**
 * @author Anton Ermak
 */
object Telegram {

    private implicit val telegramKeyboardAdapter = TelegramKeyboardAdapter

    private val ReplyMarkup: String = "reply_markup"

    def sendWebcamPreviews(previews: WebcamPreviewList, chatId: Int): Unit = {
        previews.webcams.foreach { webcam =>
            sendPhoto(downloadFromUrl(webcam.previewUrl), webcam.title, chatId)
        }
    }

    def sendMessageAndPreserveKeyboard(text: String, chatId: Int): Unit = {
        prepareSendMessage(text, chatId)
            .asString
            .body
    }

    def sendMessage(text: String, chatId: Int): Unit = {
        prepareSendMessage(text, chatId)
            .param(ReplyMarkup, JsObject("hide_keyboard" -> JsBoolean(true)).compactPrint)
            .asString
            .body
    }

    def sendMessage(text: String, chatId: Int, replyKeyboard: Keyboard): Unit = {
        prepareSendMessage(text, chatId)
            .param(ReplyMarkup, replyKeyboard.toJson.compactPrint)
            .asString
            .body
    }

    def sendErrorInfo(chatId: Int): Unit = {
        Telegram.sendMessage("Something went wrong :( Try to use me at another time.", chatId)
    }

    private def prepareSendMessage(text: String, chatId: Int) = {
        val params: Map[String, String] = Map(
            "chat_id" -> chatId.toString,
            "text" -> text,
            "disable_web_page_preview" -> "true"
        )
        prepareRequest("sendMessage", params).postForm
    }

    private def sendPhoto(content: Array[Byte], caption: String, chatId: Int): Unit = {
        val part = MultiPart("photo", s"$caption.jpg", "image/jpeg", content)
        prepareRequest("sendPhoto", Map("caption" -> caption, "chat_id" -> chatId.toString))
            .postMulti(part)
            .asString
    }

    private def prepareRequest(methodName: String, content: Map[String, String]) = {
        Http(s"https://api.telegram.org/${ConfigManager.getBotId }/$methodName").params(content)
    }

    private def downloadFromUrl(url: String): Array[Byte] = {
        Http(url).asBytes.body
    }

    case class Keyboard(buttons: Seq[Seq[String]], oneTimeKeyboard: Boolean)

}
