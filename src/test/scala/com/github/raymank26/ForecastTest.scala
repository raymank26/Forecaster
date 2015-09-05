package com.github.raymank26

import com.github.raymank26.adapters.forecast.{DataPointJsonAdapter, WeatherAdapter}
import com.github.raymank26.model.forecast.{DataPoint, Weather}

import org.scalatest.{FunSuite, Matchers}
import spray.json._

import scala.io.Source

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class ForecastTest extends FunSuite with Matchers {

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

    private def readFile(filename: String) = {
        Source.fromInputStream(getClass.getResourceAsStream(filename)).mkString
    }
}
