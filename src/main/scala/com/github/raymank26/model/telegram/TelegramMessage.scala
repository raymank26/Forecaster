package com.github.raymank26.model.telegram

import org.joda.time.DateTime

/**
 * @author Anton Ermak
 */
case class TelegramMessage(id: Int, from: TelegramUser, date: DateTime,
                           content: TelegramMessage.Content)

object TelegramMessage {

    trait Content

    case class Text(text: String) extends Content

    case class Location(latitude: Double, longitude: Double) extends Content

}

