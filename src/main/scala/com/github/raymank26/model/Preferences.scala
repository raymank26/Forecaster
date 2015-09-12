package com.github.raymank26.model

import akka.http.scaladsl.model.DateTime

/**
 * @author Anton Ermak
 */
case class Preferences(forecastReportDatetime: DateTime,
                       latitude: Double,
                       longitude: Double)
