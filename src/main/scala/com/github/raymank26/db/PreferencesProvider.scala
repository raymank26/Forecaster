package com.github.raymank26.db

import com.github.raymank26.model.Preferences
import com.github.raymank26.model.telegram.TelegramUser

/**
 * @author Anton Ermak
 */
trait PreferencesProvider {

    /**
     * Returns forecast preferences of [[com.github.raymank26.model.telegram.TelegramUser]]
     * instance if possible.
     *
     * @param chatId user's telegram identifier
     * @return forecast preferences option
     */
    def getPreferences(chatId: Int): Option[Preferences]

    /**
     * Saves forecast preferences
     *
     * @param user telegram user
     * @param prefs settings to save
     */
    def savePreferences(user: TelegramUser, prefs: Preferences)
}

