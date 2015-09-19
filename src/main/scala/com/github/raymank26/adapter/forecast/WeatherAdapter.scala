package com.github.raymank26.adapter.forecast

import com.github.raymank26.model.forecast.{DataBlock, DataPoint, Weather}

import spray.json.{JsValue, RootJsonReader}

/**
 * JSON parser of forecast.io response.
 *
 * @author Anton Ermak
 */
object WeatherAdapter extends RootJsonReader[Weather] {
    override def read(json: JsValue): Weather = {
        val jsonObject = json.asJsObject.fields

        Weather(currently = jsonObject("currently").convertTo[DataPoint](DataPointJsonAdapter),
            hourly = jsonObject("hourly").convertTo[DataBlock](DataBlockAdapter))
    }
}
