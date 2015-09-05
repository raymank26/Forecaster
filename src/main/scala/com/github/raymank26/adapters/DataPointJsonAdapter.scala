package com.github.raymank26.adapters

import com.github.raymank26.model.forecast.DataPoint
import com.github.raymank26.model.forecast.DataPoint.Icon.Icon
import com.github.raymank26.model.forecast.DataPoint.PrecipitationType.PrecipitationType
import com.github.raymank26.model.forecast.DataPoint.{Icon, PrecipitationType}

import org.joda.time.DateTime
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, JsonReader, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object DataPointJsonAdapter extends RootJsonReader[DataPoint] {

    override def read(json: JsValue): DataPoint = {

        val jsonObject = json.asJsObject.fields
        new DataPoint(
            time = getDateTime(jsonObject("time")),
            summary = jsonObject("summary").convertTo[String],
            icon = jsonObject("icon").convertTo[Icon](IconReader),
            precipitationType = jsonObject.get("precipType").map(
                _.convertTo[PrecipitationType](PrecipitationReader)),
            temperature = jsonObject("temperature").convertTo[Double],
            apparentTemperature = jsonObject("apparentTemperature").convertTo[Double]
        )
    }

    private def getDateTime(value: JsValue): DateTime = {
        value.convertTo[DateTime](DateTimeAdapter)
    }

    private object IconReader extends JsonReader[Icon] {
        override def read(json: JsValue): Icon = json.convertTo[String] match {
            case "rain" => Icon.Rain
            case "clear-day" => Icon.ClearDay
            case "clear-night" => Icon.ClearNight
            case "snow" => Icon.Snow
            case "sleet" => Icon.Sleet
            case "wind" => Icon.Wind
            case "fog" => Icon.Fog
            case "cloudy" => Icon.Cloudy
            case _ => Icon.Unknown
        }
    }

    private object PrecipitationReader extends JsonReader[PrecipitationType] {
        override def read(json: JsValue): PrecipitationType = json.convertTo[String] match {
            case "rain" => PrecipitationType.Rain
            case "snow" => PrecipitationType.Snow
            case "sleet" => PrecipitationType.Sleet
            case "hail" => PrecipitationType.Hail
            case value => throw new Exception(s"no such precipitation value for $value")
        }
    }

}
