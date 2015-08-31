package com.github.raymank26.model

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class Message(id: Int, from: User, chat: Group, date: Int, text: String)
