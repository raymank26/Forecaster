package com.github.raymank26.controller

import com.github.raymank26.ConfigManager
import com.github.raymank26.adapters.telegram.TelegramKeyboardAdapter
import com.github.raymank26.model.webcams.WebcamPreviewList

import spray.json._

import scalaj.http.{Http, MultiPart}

/**
 * @author Anton Ermak
 */
object Telegram {

    implicit val telegramKeyboardAdapter = TelegramKeyboardAdapter

    def sendWebcamPreviews(previews: WebcamPreviewList, chatId: Int): Unit = {
        previews.webcams.foreach { webcam =>
            sendPhoto(downloadFromUrl(webcam.previewUrl), webcam.title, chatId)
        }
    }

    def sendMessage(text: String, chatId: Int): Unit = {
        prepareSendMessage(text, chatId)
            .asString
            .body
    }

    def sendMessage(text: String, chatId: Int, replyKeyboard: Keyboard): Unit = {
        prepareSendMessage(text, chatId)
            .param("reply_markup", replyKeyboard.toJson.compactPrint)
            .asString
            .body
    }

    private def prepareSendMessage(text: String, chatId: Int) = {
        val params: Map[String, String] = Map(
            "chat_id" -> chatId.toString,
            "text" -> text
        )
        prepareRequest("sendMessage", params).postForm
    }

    private def sendPhoto(content: Array[Byte], caption: String, chatId: Int): Unit = {
        val part = MultiPart("photo", "photo.jpg", "image/jpeg", content)
        prepareRequest("sendPhoto", Map("caption" -> caption, "chat_id" -> chatId.toString))
            .postMulti(part)
            .asString
    }

    private def prepareRequest(methodName: String, content: Map[String, String]) = {
        Http(s"https://api.telegram.org/${ConfigManager.getBotId}/$methodName").params(content)
    }

    private def downloadFromUrl(url: String): Array[Byte] = {
        Http(url).asBytes.body
    }

    case class Keyboard(buttons: Seq[Seq[String]], oneTimeKeyboard: Boolean)

}
