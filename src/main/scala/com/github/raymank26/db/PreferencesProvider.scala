package com.github.raymank26.db

import com.github.raymank26.actor.SettingsFSM
import com.github.raymank26.model.telegram.TelegramUser

/**
 * @author Anton Ermak
 */
trait PreferencesProvider {

    /**
     * Returns forecast preferences of [[com.github.raymank26.model.telegram.TelegramUser]] instance if possible.
     *
     * @param telegramUser user instance
     * @return forecast preferences option
     */
    def getPreferences(telegramUser: TelegramUser): Option[SettingsFSM.Preferences]

    /**
     * Saves forecast preferences
     *
     * @param user telegram user
     * @param prefs settings to save
     */
    def savePreferences(user: TelegramUser, prefs: SettingsFSM.Preferences)
}

