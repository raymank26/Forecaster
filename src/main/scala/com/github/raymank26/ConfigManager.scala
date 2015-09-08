package com.github.raymank26

import com.typesafe.config.ConfigFactory

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
object ConfigManager {

    val config = ConfigFactory.load()

    def getBotId: String = config.getString("forecaster.telegram.api-key")

}
