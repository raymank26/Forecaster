package com.github.raymank26.model

import com.github.raymank26.controller.Forecast.GeoPrefs

import scala.collection.mutable.ArrayBuffer

/**
 * @author Anton Ermak
 */
case class Preferences private(language: String, geo: GeoPrefs, webcams: Seq[String])

object Preferences {

    class Builder() {
        var geo: GeoPrefs = _
        private var language: String = _
        private var webcams = ArrayBuffer[String]()

        def setLanguage(language: String): Builder = {
            this.language = language
            this
        }

        def setGeo(geo: GeoPrefs): Builder = {
            this.geo = geo
            this
        }

        def addWebcam(id: String) = {
            webcams += id
            this
        }

        def build(): Preferences = {
            Preferences(language, geo, webcams.toSeq)
        }
    }

}
