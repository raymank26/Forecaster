package com.github.raymank26.controller

import com.github.raymank26.ConfigManager
import com.github.raymank26.adapters.webcams.WebcamPreviewListAdapter
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.model.webcams.WebcamPreviewList

import spray.json._

import scala.collection.immutable.HashMap
import scalaj.http.Http

/**
 * @author Anton Ermak
 */
object Webcams {

    implicit val previewListAdapter = WebcamPreviewListAdapter

    private val defaultParams = HashMap(
        "devid" -> ConfigManager.getWebcamApiKey,
        "format" -> "json"
    )

    def getLinks(geo: GeoPrefs, webcamsIds: Seq[String]) = {
        val params = defaultParams ++ HashMap(
            "method" -> "wct.webcams.get_details_multiple",
            "webcamids" -> webcamsIds.mkString(",")
        )
        Http(s"http://api.webcams.travel/rest")
            .params(params)
            .asString
            .body
            .parseJson
            .convertTo[WebcamPreviewList]
    }

    def getLinks(geo: GeoPrefs): WebcamPreviewList = {

        val params = defaultParams ++ HashMap(
            "method" -> "wct.webcams.list_nearby",
            "lat" -> geo.latitude.toString,
            "lng" -> geo.longitude.toString
        )

        Http(s"http://api.webcams.travel/rest")
            .params(params)
            .asString
            .body
            .parseJson
            .convertTo[WebcamPreviewList]
    }

}
