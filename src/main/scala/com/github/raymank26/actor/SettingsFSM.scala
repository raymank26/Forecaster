package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM._
import com.github.raymank26.controller.Telegram.Keyboard
import com.github.raymank26.controller.{Telegram, Webcams}
import com.github.raymank26.db.{Database, PreferencesProvider}
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.Preferences.{Location => GeoLocation}
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.webcams.WebcamPreviewList

import akka.actor.{Actor, ActorRef, ActorRefFactory, FSM, Props}

import scala.util.Try

/**
 * This actor builds [[Preferences]] instance from quiz-like questions and answers.
 *
 * @author Anton Ermak.
 */
private final class SettingsFSM(parent: ActorRef, conversation: Conversation,
                                webcamProvider: WebcamProvider,
                                preferencesProvider: PreferencesProvider)
    extends Actor with FSM[SettingsState, Preferences.Builder] {

    private var webcams: WebcamPreviewList = _

    startWith(OnHello, new Preferences.Builder)

    log.debug(s"initial state, chat_id = ${conversation.chatId }")

    conversation.sayHello()

    /**
     * Waiting for location.
     */
    when(OnHello) {
        case Event(msg: TelegramMessage, data) if msg.isLocation =>
            log.debug(s"location received $msg")

            val location = msg.content.asInstanceOf[Location]
            data.setGeo(GeoLocation(location.latitude, location.longitude))
            goto(OnLocation).using(data)
        case _ => repeat()
    }

    /**
     * Waiting for language.
     */
    when(OnLocation) {
        case Event(msg: TelegramMessage, data) if getLanguage(msg).isDefined =>
            log.debug(s"language received $msg")

            data.setLanguage(getLanguage(msg).get)
            goto(IsWebcamNeeded).using(data)
        case _ => repeat()
    }

    /**
     * Waiting for yes/no answers about webcams' preview feature.
     */
    when(IsWebcamNeeded) {
        case Event(TelegramMessage(_, from, _, msg: Text), data) =>
            msg.text match {
                case TextYes =>
                    log.debug(s"Webcams is needed $msg")
                    goto(OnWebcam)
                case TextNo =>
                    log.debug(s"Webcams isn't needed $msg")
                    self ! from
                    goto(OnEnd)
            }
        case _ => repeat()
    }

    /**
     * Waiting for webcam identifier.
     */
    when(OnWebcam) {
        case Event(msg: TelegramMessage, data) =>
            getWebcamIdentifier(msg) match {
                case Right(None) =>
                    log.debug(s"User say Stop $msg")
                    self ! msg.from
                    goto(OnEnd)
                case Right(Some(num)) =>
                    if (num < webcams.webcams.length) {
                        log.debug(s"User say number $msg")
                        data.addWebcam(webcams.webcams(num).id)
                        conversation.requestAnotherWebcam()
                        stay()
                    } else {
                        repeat()
                    }
                case Left(()) =>
                    repeat()
            }
        case _ => goto(OnWebcam)
    }

    /**
     * Saving preferences.
     */
    when(OnEnd) {
        case Event(user: TelegramUser, data) =>
            preferencesProvider.savePreferences(user, data.build())
            parent ! SettingsSaved(conversation.chatId)
            context.stop(self)
            stay()
    }

    onTransition {
        case OnHello -> OnLocation => conversation.requestLanguage()
        case OnLocation -> IsWebcamNeeded =>
            conversation.isWebcamNeeded()
        case IsWebcamNeeded -> OnWebcam =>
            webcams = webcamProvider(stateData.geo)
            conversation.requestWebcams(webcams)
        case _ -> OnEnd =>
            conversation.sayGoodbye()
        // remove actor. Notify watcher
        case a -> b =>
            log.warning(s"No transition from $a to $b")
    }

    initialize()

    private def getLanguage(msg: TelegramMessage): Option[String] = {
        msg.content match {
            case msg @ Text(TextRu) => Some(msg.text)
            case msg @ Text(TextEn) => Some(msg.text)
            case _ => None
        }
    }

    private def getWebcamIdentifier(msg: TelegramMessage): Either[Unit, Option[Int]] = {
        def isNumber(str: String): Boolean = Try(str.toInt).toOption.isDefined

        msg.content match {
            case Text(str) if isNumber(str) => Right(Some(str.toInt))
            case Text(TextStop) => Right(None)
            case _ => Left(())
        }
    }

    private def repeat(): State = {
        log.debug("illegal message received")
        conversation.sayRetry(stateName)
        stay()
    }
}

private object SettingsFSM {

    val TextStop = "Stop"

    val TextEn = "en"
    val TextRu = "ru"

    val TextYes = "Yes"
    val TextNo = "No"

    def apply(chatId: Int, parent: ActorRef, context: ActorRefFactory) = {
        context.actorOf(Props(classOf[SettingsFSM], parent, new Conversation(chatId),
            new WebcamProvider, Database))
    }

    def apply(parent: ActorRef, context: ActorRefFactory, conversation: Conversation,
              webcamProvider: WebcamProvider, preferencesProvider: PreferencesProvider) = {

        context.actorOf(Props(classOf[SettingsFSM], parent, conversation, webcamProvider,
            preferencesProvider))
    }

    sealed trait SettingsState

    class WebcamProvider extends (GeoLocation => WebcamPreviewList) {
        override def apply(v1: GeoLocation): WebcamPreviewList = Webcams.getLinks(v1)
    }

    /**
     * Collection of available messages.
     *
     * @param chatId interlocutor's id
     */
    class Conversation(val chatId: Int) {

        /**
         * Welcome message.
         */
        def sayHello(): Unit =
            Telegram.sendMessage("Hi! Let's send me your location settings", chatId)

        /**
         * Handler for mistaken input
         * @param state current [[SettingsFSM]] state.
         */
        def sayRetry(state: SettingsState): Unit = {
            Telegram.sendMessage("Try another one", chatId)
        }

        /**
         * Requests another webcam.
         */
        def requestAnotherWebcam(): Unit = {
            Telegram.sendMessageAndPreserveKeyboard("Another one?", chatId)
        }

        /**
         * Requests language.
         */
        def requestLanguage(): Unit =
            Telegram.sendMessage("The next is language", chatId,
                replyKeyboard = Keyboard(buttons = Seq(Seq("en", "ru")), oneTimeKeyboard = true))

        /**
         * Sends webcams' previews and sets special keyboard.
         *
         * @param webcams available webcams based on current user's location
         */
        def requestWebcams(webcams: WebcamPreviewList): Unit = {
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

        /**
         * Checks if the user want to receive webcams previews near to current location
         */
        def isWebcamNeeded(): Unit = {
            Telegram.sendMessage("Do you want to see webcams nearly?", chatId,
                Telegram.Keyboard(Seq(Seq(TextYes, TextNo)), oneTimeKeyboard = true))
        }

        /**
         * Sends goodbye message.
         */
        def sayGoodbye(): Unit = {
            Telegram.sendMessage(s"Saved! Try to use ${CommandProcessor.CurrentCommand }", chatId)
        }
    }

    case object OnHello extends SettingsState

    case object OnLocation extends SettingsState

    case object OnLanguage extends SettingsState

    case object IsWebcamNeeded extends SettingsState

    case object OnWebcam extends SettingsState

    case object OnEnd extends SettingsState

}
