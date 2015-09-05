package com.github.raymank26

import com.typesafe.config.ConfigFactory

import spray.json._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object Forecast {

    private val apiParams = getForecastConfig

    def getCurrentForecast = {
        val url = s"https://api.forecast.io/forecast/${apiParams.apiKey}/${
            apiParams.latitude
        },${apiParams.longitude}"
        val str = scalaj.http.Http(url).param("units", "si").param("lang", "ru").asString
        parseResponse(str.body)
    }

    private def getForecastConfig: Params = {
        val typesafeConfig = ConfigFactory.load()

        def read(key: String) = typesafeConfig.getString(s"forecast.$key")

        Params(read("latitude").toDouble, read("longitude").toDouble, read("apiKey"))
    }

    private def parseResponse(body: String) = {
        body.parseJson.asJsObject()
    }

    private case class Params(latitude: Double, longitude: Double, apiKey: String)

}
