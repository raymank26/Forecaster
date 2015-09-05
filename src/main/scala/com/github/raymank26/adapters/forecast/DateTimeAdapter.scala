package com.github.raymank26.adapters.forecast

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object DateTimeAdapter extends RootJsonReader[DateTime] {
    override def read(json: JsValue): DateTime = {
        new DateTime(json.convertTo[Long] * 1000L)
    }
}
