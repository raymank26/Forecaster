package com.github.raymank26.model

import org.joda.time.DateTime

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class User(id: Option[Int], name: String, messageDatetime: DateTime);
