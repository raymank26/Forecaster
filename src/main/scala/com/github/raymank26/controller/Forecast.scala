package com.github.raymank26.controller

import com.github.raymank26.adapters.forecast.WeatherAdapter
import com.github.raymank26.model.forecast.Weather

import com.typesafe.config.ConfigFactory

import spray.json._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Forecast {

    implicit val weatherAdapter = WeatherAdapter

    private val forecastApiKey: String = {
        val typesafeConfig = ConfigFactory.load()
        typesafeConfig.getString(s"forecaster.forecast.api-key")
    }

    def getCurrentForecast(settings: ForecastUserSettings) = {
        parseResponse(makeRequest(settings).body)
    }

    private def parseResponse(body: String) = {
        body.parseJson.convertTo[Weather]
    }

    private def makeRequest(settings: ForecastUserSettings) = {
        val url = s"""https://api.forecast.io/
                     |forecast/$forecastApiKey/${settings.latitude},${settings.longitude}"""
            .stripMargin.replace("\n", "")
        scalaj.http.Http(url)
            .param("units", "si")
            .param("lang", "ru")
            .asString
    }

    case class ForecastUserSettings(latitude: Double, longitude: Double)

}
