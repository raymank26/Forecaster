package com.github.raymank26.adapter.forecast

import com.github.raymank26.adapter.forecast.DataPointJsonAdapter.IconReader
import com.github.raymank26.model.forecast.DataPoint.IconType.Icon
import com.github.raymank26.model.forecast.{DataBlock, DataPoint}

import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsValue, RootJsonReader}

/**
 * JSON parser of forecast.io [[DataBlock]].
 *
 * @author Anton Ermak
 */
object DataBlockAdapter extends RootJsonReader[DataBlock] {

    override def read(json: JsValue): DataBlock = {
        val jsonObject = json.asJsObject.fields
        DataBlock(summary = jsonObject("summary").convertTo[String],
            icon = jsonObject("icon").convertTo[Icon](IconReader),
            data = getData(jsonObject("data").convertTo[JsArray]))
    }

    private def getData(value: JsArray): Seq[DataPoint] = {
        value.elements.map { item =>
            item.convertTo[DataPoint](DataPointJsonAdapter)
        }
    }
}
