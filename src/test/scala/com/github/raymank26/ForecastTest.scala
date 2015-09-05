package com.github.raymank26

import com.github.raymank26.adapters.DataPointJsonAdapter
import com.github.raymank26.model.forecast.DataPoint

import org.scalatest.{FunSuite, Matchers}
import spray.json._

import scala.io.Source

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class ForecastTest extends FunSuite with Matchers {

    test("parsing forecast data point") {
        val jsonDataPoint = Source.fromInputStream(
            getClass.getResourceAsStream("/datapoint.json")).mkString
        val dataPoint = JsonParser(jsonDataPoint).convertTo[DataPoint](DataPointJsonAdapter)

        println(dataPoint)


    }


}
