package com.github.raymank26.controller

import com.github.raymank26.adapters.forecast.WeatherAdapter
import com.github.raymank26.model.forecast.Weather

import com.typesafe.config.ConfigFactory

import spray.json._

/**
 * @author Anton Ermak
 */
object Forecast {

    implicit val weatherAdapter = WeatherAdapter

    private val forecastApiKey: String = {
        val typesafeConfig = ConfigFactory.load()
        typesafeConfig.getString(s"forecaster.forecast.api-key")
    }

    def getCurrentForecast(settings: GeoPrefs, lang: String) = {
        parseResponse(makeRequest(settings, lang).body)
    }

    private def parseResponse(body: String) = {
        body.parseJson.convertTo[Weather]
    }

    private def makeRequest(settings: GeoPrefs, lang: String) = {
        val url =
            s"""https://api.forecast.io/
               |forecast/$forecastApiKey/${settings.latitude},${settings.longitude}"""
                .stripMargin.replace("\n", "")

        scalaj.http.Http(url)
            .param("units", "si")
            .param("lang", lang.toLowerCase)
            .asString
    }

    case class GeoPrefs(latitude: Double, longitude: Double)

}
