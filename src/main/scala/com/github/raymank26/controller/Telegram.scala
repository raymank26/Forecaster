package com.github.raymank26.controller

import com.github.raymank26.ConfigManager

import scalaj.http.Http

/**
 * @author Anton Ermak
 */
object Telegram {

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
