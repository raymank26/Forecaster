package com.github.raymank26.adapter.webcam

import com.github.raymank26.model.webcam.{Webcam, WebcamPreviewList}

import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsValue, RootJsonReader}

/**
 * @author Anton Ermak
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
