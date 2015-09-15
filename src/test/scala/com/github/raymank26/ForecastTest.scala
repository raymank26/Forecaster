package com.github.raymank26

import com.github.raymank26.adapters.forecast.{DataPointJsonAdapter, WeatherAdapter}
import com.github.raymank26.model.forecast.{DataPoint, Weather}

import spray.json._

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class ForecastTest extends Suite {

    test("parsing forecast data point") {
        val dataPoint = JsonParser(readFile("/datapoint.json"))
            .convertTo[DataPoint](DataPointJsonAdapter)

        println(dataPoint)
    }

    test("weather parsing") {
        val weather = JsonParser(readFile("/weather.json"))
            .convertTo[Weather](WeatherAdapter)

        println(weather.hourly.data.length)
    }

    test("fetch forecast info") {
    }
}
