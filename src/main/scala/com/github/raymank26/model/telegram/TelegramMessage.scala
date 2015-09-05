package com.github.raymank26.model.telegram

import org.joda.time.DateTime

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class TelegramMessage(id: Int, from: TelegramUser, date: DateTime, text: String)
