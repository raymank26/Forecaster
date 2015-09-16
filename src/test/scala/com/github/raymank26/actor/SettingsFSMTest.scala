package com.github.raymank26.actor

import com.github.raymank26.AkkaSuite
import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM.{Conversation, SettingsState, WebcamProvider}
import com.github.raymank26.actor.SettingsFSMTest._
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.db.PreferencesProvider
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.webcams.{Webcam, WebcamPreviewList}

import org.joda.time.DateTime

/**
 * @author Anton Ermak
 */
final class SettingsFSMTest extends AkkaSuite {

    private val mockedConversation = new Conversation(ChatId) {

        override def sayGoodbye(): Unit = self ! SayGoodbye

        override def requestWebcams(webcams: WebcamPreviewList): Unit = self ! RequestWebcams

        override def requestLanguage(): Unit = self ! RequestLanguage

        override def requestAnotherWebcam(): Unit = self ! RequestAnotherWebcam

        override def sayRetry(state: SettingsState): Unit = self ! Retry(state)

        override def sayHello(): Unit = {
            self ! Hello
        }
    }

    private val mockedPreferencesProvider = new PreferencesProvider {

        override def savePreferences(user: TelegramUser, prefs: Preferences): Unit = ()

        override def getPreferences(telegramUser: TelegramUser): Option[Preferences] = None
    }

    test("plain workflow") {
        val settingsRef = SettingsFSM(self, system, mockedConversation, mockedWebcamProvider,
            mockedPreferencesProvider)

        expectMsg(Hello)
        settingsRef ! createTelegramMessage(Location(10, 11))

        expectMsg(RequestLanguage)
        settingsRef ! createTelegramMessage(Text("ru"))

        expectMsg(RequestWebcams)
        settingsRef ! createTelegramMessage(Text("0"))

        expectMsg(RequestAnotherWebcam)
        settingsRef ! createTelegramMessage(Text("1"))

        expectMsg(RequestAnotherWebcam)
        settingsRef ! createTelegramMessage(Text("Stop"))

        expectMsg(SayGoodbye)
        expectMsg(SettingsSaved(ChatId))
    }

    test("mistaken workflow") {
        val settingsRef = SettingsFSM(self, system, mockedConversation, mockedWebcamProvider,
            mockedPreferencesProvider)

        expectMsg(Hello)
        settingsRef ! createTelegramMessage(Text("error input"))

        expectMsg(Retry(SettingsFSM.OnHello))
        settingsRef ! createTelegramMessage(Location(10, 11))

        expectMsg(RequestLanguage)
        settingsRef ! createTelegramMessage(Text("ru"))

        expectMsg(RequestWebcams)
        settingsRef ! createTelegramMessage(Text("0"))

        expectMsg(RequestAnotherWebcam)
        settingsRef ! createTelegramMessage(Text("1"))

        expectMsg(RequestAnotherWebcam)
        settingsRef ! createTelegramMessage(Text("Stop"))

        expectMsg(SayGoodbye)
        expectMsg(SettingsSaved(ChatId))

    }
}

private object SettingsFSMTest {

    val ChatId = 55

    val mockedWebcamProvider = new WebcamProvider {
        private val webcamSize = 5

        override def apply(v1: GeoPrefs): WebcamPreviewList =
            WebcamPreviewList(List.fill(webcamSize)(new Webcam("foo", "bar", "some-id")))
    }

    def createTelegramMessage(content: TelegramMessage.Content) = {
        TelegramMessage(0, TelegramUser("antonermak", ChatId), new DateTime, content)
    }

    sealed trait ExpectedMessages

    case class Retry(state: SettingsState) extends ExpectedMessages

    case object Hello extends ExpectedMessages

    case object RequestAnotherWebcam extends ExpectedMessages

    case object RequestWebcams extends ExpectedMessages

    case object RequestLanguage extends ExpectedMessages

    case object SayGoodbye extends ExpectedMessages

}
