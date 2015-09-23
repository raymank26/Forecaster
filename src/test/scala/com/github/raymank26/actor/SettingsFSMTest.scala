package com.github.raymank26.actor

import com.github.raymank26.AkkaSuite
import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM.{Conversation, SettingsState, WebcamProvider}
import com.github.raymank26.actor.SettingsFSMTest._
import com.github.raymank26.db.PreferencesProvider
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.Preferences.{Location => GeoLocation}
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.webcam.{Webcam, WebcamPreviewList}

import akka.actor.ActorRef
import org.joda.time.DateTime

/**
 * @author Anton Ermak
 */
final class SettingsFSMTest extends AkkaSuite {

    private val mockedConversation = new Conversation(ChatId) {

        override def sayGoodbye(saved: Boolean): Unit = self ! SayGoodbye

        override def requestWebcams(webcams: WebcamPreviewList): Unit = self ! RequestWebcams

        override def requestAnotherWebcam(): Unit = self ! RequestAnotherWebcam

        override def sayRetry(state: SettingsState): Unit = self ! Retry(state)

        override def requestLocation(): Unit = self ! Hello

        override def requestProceed(prefs: Preferences): Unit = self ! RequestPrefs

        override def isWebcamNeeded(): Unit = self ! IsWebcamNeeded
    }

    private def sendWebcams(settingsRef: ActorRef): Unit = {
        0 until Webcams foreach { _ =>
            settingsRef ! createTelegramMessage(Text("0"))
            expectMsg(RequestAnotherWebcam)
        }
        settingsRef ! createTelegramMessage(Text(SettingsFSM.TextStop))
    }

    class MockedPreferencesProvider(lengthValue: Int) extends PreferencesProvider {
        override def savePreferences(user: TelegramUser, prefs: Preferences): Unit = {
            prefs.webcams should have length lengthValue
        }

        override def getPreferences(chatId: Int): Option[Preferences] = None
    }

    test("plain workflow") {
        val settingsRef = SettingsFSM(self, system, mockedConversation, mockedWebcamProvider,
            new MockedPreferencesProvider(Webcams))

        expectMsg(Hello)
        settingsRef ! createTelegramMessage(Location(10, 11))

        expectMsg(IsWebcamNeeded)
        settingsRef ! createTelegramMessage(Text(SettingsFSM.TextYes))

        expectMsg(RequestWebcams)
        sendWebcams(settingsRef)

        expectMsg(SayGoodbye)
        expectMsg(SettingsSaved(ChatId))
    }

    test("mistaken workflow") {
        val settingsRef = SettingsFSM(self, system, mockedConversation, mockedWebcamProvider,
            new MockedPreferencesProvider(Webcams))

        expectMsg(Hello)
        settingsRef ! createTelegramMessage(Text("error input"))

        expectMsg(Retry(SettingsFSM.OnProceed))
        settingsRef ! createTelegramMessage(Location(10, 11))

        expectMsg(IsWebcamNeeded)
        settingsRef ! createTelegramMessage(Text(SettingsFSM.TextYes))

        expectMsg(RequestWebcams)
        sendWebcams(settingsRef)

        expectMsg(SayGoodbye)
        expectMsg(SettingsSaved(ChatId))
    }

    test("no webcams needed workflow") {
        val settingsRef = SettingsFSM(self, system, mockedConversation, mockedWebcamProvider,
            new MockedPreferencesProvider(0))

        expectMsg(Hello)
        settingsRef ! createTelegramMessage(Text("error input"))

        expectMsg(Retry(SettingsFSM.OnProceed))
        settingsRef ! createTelegramMessage(Location(10, 11))

        expectMsg(IsWebcamNeeded)
        settingsRef ! createTelegramMessage(Text(SettingsFSM.TextNo))

        expectMsg(SayGoodbye)
        expectMsg(SettingsSaved(ChatId))

    }
}

private object SettingsFSMTest {

    val ChatId = 55

    val Webcams = 2

    val mockedWebcamProvider = new WebcamProvider {
        private val webcamSize = 5

        override def apply(v1: GeoLocation): WebcamPreviewList =
            WebcamPreviewList(List.fill(webcamSize)(new Webcam("foo", "bar", "some-id")))
    }

    def createTelegramMessage(content: TelegramMessage.Content) = {
        TelegramMessage(0, TelegramUser("anton", None, ChatId), new DateTime, content)
    }

    sealed trait ExpectedMessages

    case class Retry(state: SettingsState) extends ExpectedMessages

    case object RequestPrefs extends ExpectedMessages

    case object Hello extends ExpectedMessages

    case object RequestAnotherWebcam extends ExpectedMessages

    case object RequestWebcams extends ExpectedMessages

    case object IsWebcamNeeded extends ExpectedMessages

    case object RequestLanguage extends ExpectedMessages

    case object SayGoodbye extends ExpectedMessages

}
