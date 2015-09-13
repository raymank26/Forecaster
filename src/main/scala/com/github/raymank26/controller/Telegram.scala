package com.github.raymank26.controller

import com.github.raymank26.ConfigManager
import com.github.raymank26.model.webcams.WebcamPreviewList

import scalaj.http.{Http, MultiPart}

/**
 * @author Anton Ermak
 */
object Telegram {

    def sendWebcamPreviews(previews: WebcamPreviewList, chatId: Int): Unit = {
        previews.webcams.foreach { webcam =>
            sendPhoto(downloadFromUrl(webcam.previewUrl), webcam.title, chatId)
        }
    }

    def sendMessage(text: String, chatId: Int): Unit = {
        prepareRequest("sendMessage", Map(
            "chat_id" -> chatId.toString,
            "text" -> text
        )).postForm.asString
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
}
