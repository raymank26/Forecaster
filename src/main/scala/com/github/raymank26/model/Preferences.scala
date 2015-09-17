package com.github.raymank26.model

import com.github.raymank26.model.Preferences.Location

import scala.collection.mutable.ArrayBuffer

/**
 * @author Anton Ermak
 */
case class Preferences private(language: String, geo: Location, webcams: Seq[String])

object Preferences {

    class Builder() {
        var geo: Location = _
        private var language: String = _
        private var webcams = ArrayBuffer[String]()

        def setLanguage(language: String): Builder = {
            this.language = language
            this
        }

        def setGeo(geo: Location): Builder = {
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

    case class Location(latitude: Double, longitude: Double)

}
