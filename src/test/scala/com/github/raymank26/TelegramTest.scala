package com.github.raymank26

import com.github.raymank26.adapters.telegram.TelegramMessageAdapter
import com.github.raymank26.model.telegram.TelegramMessage

import spray.json.JsonParser


/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class TelegramTest extends Suite {

    private implicit val telegramMessageAdapter = TelegramMessageAdapter

    test("message with location content parsing") {
        val str = readFile("/telegram/message_location.json")
        val message = JsonParser(str).convertTo[TelegramMessage]
    }

    test("message text parsing") {
        val str = readFile("/telegram/message_text.json")
        val message = JsonParser(str).convertTo[TelegramMessage]
        message.content shouldEqual TelegramMessage.Text("hello")
    }
}
