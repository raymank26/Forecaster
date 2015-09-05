package com.github.raymank26.adapters

import com.github.raymank26.model.forecast.{DataBlock, DataPoint, Weather}

import spray.json.{JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object WeatherAdapter extends RootJsonReader[Weather] {
    override def read(json: JsValue): Weather = {
        val jsonObject = json.asJsObject.fields

        Weather(currently = jsonObject("currently").convertTo[DataPoint](DataPointJsonAdapter),
            hourly = jsonObject("hourly").convertTo[DataBlock](DataBlockAdapter))
    }
}
