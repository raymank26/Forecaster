package com.github.raymank26.model

import akka.http.scaladsl.model.DateTime

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class Preferences(forecastReportDatetime: DateTime, latitude: Double, longitude: Double)
