package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM.Preferences.Builder
import com.github.raymank26.actor.SettingsFSM.{Conversation, OnEnd, OnHello, OnLocation, OnWebcam, Preferences, SettingsState}
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.controller.Telegram.Keyboard
import com.github.raymank26.controller.{Telegram, Webcams}
import com.github.raymank26.db.Database
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.webcams.WebcamPreviewList

import akka.actor.FSM.Normal
import akka.actor.{Actor, ActorContext, ActorRef, FSM, Props}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * @author Anton Ermak.
 */
class SettingsFSM(chatId: Int, parent: ActorRef) extends Actor with FSM[SettingsState, Preferences.Builder] {

    private val conversation = new Conversation(chatId)
    private var webcams: WebcamPreviewList = _

    startWith(OnHello, new Builder)

    conversation.sendHello()

    // waiting for location
    when(OnHello) {
        case Event(msg: TelegramMessage, data) if msg.isLocation =>
            val location = msg.content.asInstanceOf[Location]
            data.setGeo(GeoPrefs(location.latitude, location.longitude))
            goto(OnLocation).using(data)
        case _ => repeat()
    }

    // waiting for language
    when(OnLocation) {
        case Event(msg: TelegramMessage, data) if getLanguage(msg).isDefined =>
            data.setLanguage(getLanguage(msg).get)
            goto(OnWebcam).using(data)
        case _ => repeat()
    }

    // waiting for webcam identifier
    when(OnWebcam) {
        case Event(msg: TelegramMessage, data) =>
            getWebcamIdentifier(msg) match {
                case Right(None) =>
                    self ! msg.from
                    goto(OnEnd)
                case Right(Some(num)) =>
                    if (num < webcams.webcams.length) {
                        data.addWebcam(webcams.webcams(num).id)
                        conversation.requestAnotherWebcam()
                        stay()
                    } else {
                        repeat()
                    }
                case Left(()) => repeat()
            }
        case _ => goto(OnWebcam)
    }

    // saving settings. Listen to self
    when(OnEnd) {
        case Event(user: TelegramUser, data) =>
            parent ! SettingsSaved(chatId)
            Database.saveSettings(user, data.build())
            stop(Normal)
    }

    onTransition {
        case OnHello -> OnLocation => conversation.sendLanguageQuestion()
        case OnLocation -> OnWebcam =>
            webcams = SettingsFSM.loadWebcams(stateData.geo)
            conversation.sayWebcamsQuestion(webcams)
        case OnWebcam -> OnEnd =>
            conversation.sayGoodbye()
        // remove actor. Notify watcher
        case from -> to if from == to => conversation.sendRetry(from)
        case a -> b => log.warning(s"No transition from $a to $b")
    }

    initialize()

    private def getLanguage(msg: TelegramMessage): Option[String] = {
        msg.content match {
            case Text("ru") => Some("ru")
            case Text("en") => Some("en")
            case _ => None
        }
    }

    private def getWebcamIdentifier(msg: TelegramMessage): Either[Unit, Option[Int]] = {
        def isNumber(str: String): Boolean = Try(str.toInt).toOption.isDefined

        msg.content match {
            case Text(str) if isNumber(str) => Right(Some(str.toInt))
            case Text("Stop") => Right(None)
            case _ => Left(())
        }
    }

    private def repeat(): State = {
        goto(stateName)
    }
}

object SettingsFSM {

    def apply(chatId: Int, parent: ActorRef, context: ActorContext) = {
        context.actorOf(Props(classOf[SettingsFSM], chatId, parent))
    }

    private def loadWebcams(geo: GeoPrefs): WebcamPreviewList = Webcams.getLinks(geo)

    sealed trait SettingsState

    case class Preferences private(language: String, geo: GeoPrefs, webcams: Seq[String])

    private class Conversation(chatId: Int) {

        def sendHello(): Unit =
            Telegram.sendMessage("Hi! Let's send me your location settings", chatId)

        def sendRetry(state: SettingsState): Unit = {
            Telegram.sendMessage("Try another one", chatId)
        }

        def requestAnotherWebcam(): Unit = {
            Telegram.sendMessage("Another one?", chatId)
        }

        def sendLanguageQuestion(): Unit =
            Telegram.sendMessage("The next is language", chatId,
                replyKeyboard = Keyboard(buttons = Seq(Seq("en", "ru")),
                    oneTimeKeyboard = true))

        def sayWebcamsQuestion(webcams: WebcamPreviewList): Unit = {
            val len = webcams.webcams.length
            val keyboardButtons = Seq
                .iterate(0, len) { _ + 1 }
                .map(_.toString)
                .:+("Stop")
                .grouped(len / 2)
                .toSeq
            Telegram.sendWebcamPreviews(webcams, chatId)
            Telegram.sendMessage("Which one?", chatId,
                replyKeyboard = Keyboard(keyboardButtons, oneTimeKeyboard = true))
        }

        def sayGoodbye(): Unit = {
            Telegram.sendMessage("Saved! Try to use /current", chatId)
        }
    }


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

    case object OnHello extends SettingsState

    case object OnLocation extends SettingsState

    case object OnLanguage extends SettingsState

    case object OnWebcam extends SettingsState

    case object OnEnd extends SettingsState

}
