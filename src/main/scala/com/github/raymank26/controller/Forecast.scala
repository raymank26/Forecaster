package com.github.raymank26.controller

import com.github.raymank26.adapters.forecast.WeatherAdapter
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.Preferences.Location
import com.github.raymank26.model.forecast.DataPoint.IconType.{ClearDay, ClearNight, Cloudy, Icon, Rain, Sleet, Snow}
import com.github.raymank26.model.forecast.{DataPoint, Weather}

import com.typesafe.config.ConfigFactory

import spray.json._

/**
 * @author Anton Ermak
 */
object Forecast {

    private implicit val weatherAdapter = WeatherAdapter

    private val forecastApiKey: String = {
        val typesafeConfig = ConfigFactory.load()
        typesafeConfig.getString(s"forecaster.forecast.api-key")
    }

    private val TodayPeriodSize = 12

    private val SnowSymbol = "\u2744"
    private val RainSymbol = "\u2614"
    private val ClearDaySymbol = "\uD83C\uDF1D"
    private val ClearNightSymbol = "\uD83C\uDF1A"
    private val CloudySymbol = "\u2601"

    /**
     * Sends forecast information according to user's preferences and required period.
     *
     * @param prefs user's preferences
     * @param chatId user's identifier
     * @param period forecast period
     */
    def sendForecast(prefs: Preferences, chatId: Int, period: Period): Unit = {

        val forecast = Forecast.getCurrentForecast(prefs.geo, prefs.language)
        if (period == Currently && prefs.webcams.nonEmpty) {
            val previews = Webcams.getLinks(prefs.geo, prefs.webcams)
            Telegram.sendWebcamPreviews(previews, chatId)
        }
        val dataPoints: Seq[DataPoint] = period match {
            case Currently => Seq(forecast.currently)
            case Today => forecast.hourly.data.take(TodayPeriodSize)
        }
        dataPoints.foreach { dataPoint =>
            Telegram.sendMessage(makeForecastMessage(dataPoint), chatId)
        }
    }

    private def getCurrentForecast(settings: Location, lang: String): Weather = {
        parseResponse(makeRequest(settings, lang).body)
    }

    private def serializeIcon(icon: Icon): String = {

        icon match {
            case Rain => RainSymbol
            case ClearDay => ClearDaySymbol
            case ClearNight => ClearNightSymbol
            case Cloudy => CloudySymbol
            case Snow => SnowSymbol
            case Sleet => SnowSymbol + RainSymbol
            case _ => ""
        }
    }

    private def makeForecastMessage(dataPoint: DataPoint): String = {
        val icon = serializeIcon(dataPoint.icon)
        // @formatter:off
        s"""
           |${dataPoint.summary} $icon
           |- temperature ${dataPoint.temperature} °C;
           |- apparent temperature ${dataPoint.apparentTemperature} °C;
           |- wind speed ${dataPoint.windSpeed} m/s.
           |- Precipitation probability ${dataPoint.precipitationProbability}.
         """.stripMargin
        // @formatter: on
    }

    private def parseResponse(body: String) = {
        body.parseJson.convertTo[Weather]
    }

    private def makeRequest(settings: Location, lang: String) = {
        val url =
            s"""https://api.forecast.io/
               |forecast/$forecastApiKey/${settings.latitude},${settings.longitude}"""
                .stripMargin.replace("\n", "")

        scalaj.http.Http(url)
            .param("units", "si")
            .param("lang", lang.toLowerCase)
            .asString
    }

    trait Period

    object Currently extends Period

    object Today extends Period

}
