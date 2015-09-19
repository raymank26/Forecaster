package com.github.raymank26.adapter.forecast

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonReader}

/**
 * JSON parser of forecast.io [[DateTime]].
 *
 * @author Anton Ermak
 */
object DateTimeAdapter extends RootJsonReader[DateTime] {
    override def read(json: JsValue): DateTime = {
        new DateTime(json.convertTo[Long] * 1000L)
    }
}
