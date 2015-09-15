package com.github.raymank26.adapters.webcams

import com.github.raymank26.model.webcams.{Webcam, WebcamPreviewList}

import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsValue, RootJsonReader}

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object WebcamPreviewListAdapter extends RootJsonReader[WebcamPreviewList] {
    override def read(json: JsValue): WebcamPreviewList = {
        val webcams = json
            .asJsObject.fields("webcams")
            .asJsObject.fields("webcam") // first page only
            .convertTo[JsArray]
        WebcamPreviewList(webcams.elements.map(parseWebcamItem).toList)
    }

    private def parseWebcamItem(item: JsValue) = {
        val webcam = item.asJsObject.fields
        Webcam(title = webcam("title").convertTo[String],
            previewUrl = webcam("preview_url").convertTo[String],
            id = webcam("webcamid").convertTo[String])
    }
}
