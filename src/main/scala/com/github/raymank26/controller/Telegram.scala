package com.github.raymank26.controller

import com.github.raymank26.ConfigManager
import com.github.raymank26.model.webcams.WebcamPreviewList

import scalaj.http.Http

/**
 * @author Anton Ermak
 */
object Telegram {

    def sendWebcamPreviews(previews: WebcamPreviewList, chatId: Int): Unit = {
        previews.webcams.foreach { webcam =>
            sendMessage(webcam.title, chatId)
            sendMessage(webcam.previewUrl, chatId)
        }
    }

    def sendMessage(text: String, chatId: Int): Unit = {
        sendRequest("sendMessage", Map(
            "chat_id" -> chatId.toString,
            "text" -> text
        ))
    }

    private def sendRequest(methodName: String, content: Map[String, String]): String = {
        Http(s"https://api.telegram.org/${ConfigManager.getBotId}/$methodName")
            .params(content)
            .postForm
            .asString
            .body
    }

}
