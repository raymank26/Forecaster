package com.github.raymank26.model.forecast

import com.github.raymank26.model.forecast.DataPoint.IconType.Icon
import com.github.raymank26.model.forecast.DataPoint.PrecipitationType.PrecipitationType

import org.joda.time.DateTime

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class DataPoint(time: DateTime,
                     summary: String,
                     icon: Icon,
                     precipitationType: Option[PrecipitationType],
                     temperature: Double,
                     apparentTemperature: Double)

object DataPoint {

    object IconType {

        sealed trait Icon

        case object Rain extends Icon

        case object ClearDay extends Icon

        case object ClearNight extends Icon

        case object Snow extends Icon

        case object Sleet extends Icon

        case object Wind extends Icon

        case object Fog extends Icon

        case object Cloudy extends Icon

        case object Unknown extends Icon

    }

    object PrecipitationType {

        sealed trait PrecipitationType

        case object Rain extends PrecipitationType

        case object Snow extends PrecipitationType

        case object Sleet extends PrecipitationType

        case object Hail extends PrecipitationType

    }

}
