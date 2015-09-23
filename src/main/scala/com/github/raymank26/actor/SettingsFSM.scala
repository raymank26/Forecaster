package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM.Conversation.YesNoKeyboard
import com.github.raymank26.actor.SettingsFSM._
import com.github.raymank26.controller.Telegram.Keyboard
import com.github.raymank26.controller.{Telegram, Webcams}
import com.github.raymank26.db.{Database, PreferencesProvider}
import com.github.raymank26.model.Preferences
import com.github.raymank26.model.Preferences.{Location => GeoLocation}
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.telegram.{TelegramMessage, TelegramUser}
import com.github.raymank26.model.webcam.WebcamPreviewList

import akka.actor.{Actor, ActorRef, ActorRefFactory, FSM, Props}

import scala.util.Try

/**
 * This actor builds [[Preferences]] instance from quiz-like questions and answers.
 *
 * @author Anton Ermak
 */
private final class SettingsFSM(parent: ActorRef, conversation: Conversation,
                                webcamProvider: WebcamProvider,
                                preferencesProvider: PreferencesProvider)

    extends Actor with FSM[SettingsState, Preferences.Builder] {

    private var webcams: WebcamPreviewList = _

    setInitialState()

    log.debug(s"initial state, chat_id = ${conversation.chatId }")

    when(OnDecide) {
        case Event(TelegramMessage(_, _, _, msg: Text), data) =>
            msg.text match {
                case TextYes => goto(OnProceed)
                case TextNo =>
                    self ! NormalExit
                    goto(OnEnd)
                case _ => repeat()
            }
        case _ => repeat()
    }

    /**
     * Waiting for location.
     */
    when(OnProceed) {
        case Event(msg: TelegramMessage, data) if msg.isLocation =>
            log.debug(s"location received $msg")

            val location = msg.content.asInstanceOf[Location]
            data.setGeo(GeoLocation(location.latitude, location.longitude))
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
                    webcams = webcamProvider(stateData.geo)
                    log.debug(s"Webcams is needed $msg")
                    if (webcams.webcams.isEmpty) {
                        conversation.noWebcamsFound()
                        self ! from
                        goto(OnEnd)
                    } else {
                        goto(OnWebcam)
                    }
                case TextNo =>
                    log.debug(s"Webcams isn't needed $msg")
                    self ! from
                    goto(OnEnd)
                case _ => repeat()
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
            selfStop()
        case Event(NormalExit, data) =>
            selfStop()
    }

    onTransition {
        case OnDecide -> OnProceed => conversation.requestLocation()
        case OnProceed -> IsWebcamNeeded =>
            conversation.isWebcamNeeded()
        case IsWebcamNeeded -> OnWebcam =>
            conversation.requestWebcams(webcams)
        case OnDecide -> OnEnd =>
            conversation.sayGoodbye(saved = false)
        case _ -> OnEnd =>
            conversation.sayGoodbye(saved = true)
        // remove actor. Notify watcher
        case a -> b =>
            log.warning(s"No transition from $a to $b")
    }

    initialize()

    private def getWebcamIdentifier(msg: TelegramMessage): Either[Unit, Option[Int]] = {
        def isNumber(str: String): Boolean = Try(str.toInt).toOption.isDefined

        msg.content match {
            case Text(str) if isNumber(str) => Right(Some(str.toInt - 1))
            case Text(TextStop) => Right(None)
            case _ => Left(())
        }
    }

    private def repeat(): State = {
        log.debug("illegal message received")
        conversation.sayRetry(stateName)
        stay()
    }

    private def setInitialState(): Unit = {
        preferencesProvider.getPreferences(conversation.chatId) match {
            case Some(prefs) =>
                conversation.requestProceed(prefs)
                startWith(OnDecide, new Preferences.Builder)
            case None =>
                conversation.requestLocation()
                startWith(OnProceed, new Preferences.Builder)
        }
    }

    private def selfStop(): State = {
        parent ! SettingsSaved(conversation.telegramUser)
        context.stop(self)
        stay()
    }

}

private object SettingsFSM {

    val TextStop = "Stop"

    val TextYes = "Yes"
    val TextNo = "No"

    def apply(user: TelegramUser, parent: ActorRef): Props = {
        Props(classOf[SettingsFSM], parent, new Conversation(user),
            new WebcamProvider, Database)
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
     * @param telegramUser interlocutor's id
     */
    class Conversation(val telegramUser: TelegramUser) {

        val chatId = telegramUser.chatId

        private var webcamKeyboard: Keyboard = _

        /**
         * Asks user for changing preferences.
         *
         * @param prefs current saved preferences
         */
        def requestProceed(prefs: Preferences): Unit = {
            val howManyWebcams = prefs.webcams.length match {
                case 0 => "no"
                case n => n
            }
            //@formatter:off
            Telegram.sendMessage(
                s"""
                    |Your preferences is:
                    |1. Location - ${prefs.geo.latitude }, ${prefs.geo.longitude}
                    |2. Webcams list contains $howManyWebcams items
                    |Do you want to replace them?
                """.stripMargin, chatId, replyKeyboard = YesNoKeyboard)
            //@formatter:on
        }

        /**
         * Welcome message.
         */
        def requestLocation(): Unit =
            Telegram.sendMessage(
                """Ok, send me your location settings.
                  |You can find this feature inside attachments tab.
                """.stripMargin, chatId)

        /**
         * Handler for mistaken input
         * @param state current [[SettingsFSM]] state.
         */
        def sayRetry(state: SettingsState): Unit = {
            Telegram.sendNotUnderstand(telegramUser)
        }

        /**
         * Requests another webcam.
         */
        def requestAnotherWebcam(): Unit = {
            Telegram.sendMessage("One more? If not, press \"Stop\".", chatId, webcamKeyboard)
        }

        /**
         * Sends webcams' previews and sets special keyboard.
         *
         * @param webcams available webcams based on current user's location
         */
        def requestWebcams(webcams: WebcamPreviewList): Unit = {
            setKeyboard(webcams)
            Telegram.sendWebcamPreviews(webcams, chatId)
            Telegram.sendMessage("Which one?", chatId, replyKeyboard = webcamKeyboard)
        }

        def noWebcamsFound(): Unit = {
            Telegram.sendMessage("Unfortunately, I can't find any webcams near to you.", chatId)
        }

        /**
         * Checks if the user want to receive webcams previews near to current location
         */
        def isWebcamNeeded(): Unit = {
            Telegram.sendMessage("Do you want to see webcams nearly?", chatId, YesNoKeyboard)
        }

        /**
         * Sends goodbye message.
         */
        def sayGoodbye(saved: Boolean): Unit = {
            if (saved) {
                Telegram.sendMessage(
                    s"Settings is saved. Try to use ${CommandProcessor.CommandCurrent }.",
                    chatId)
            } else {
                Telegram.sendMessage("Good.", chatId)
            }
        }

        private def setKeyboard(webcams: WebcamPreviewList): Unit = {
            val len = webcams.webcams.length
            val keyboardButtons = Seq
                .iterate(1, len) { _ + 1 }
                .map(_.toString)
                .:+(TextStop)
                .grouped(len / 2)
                .toSeq
            webcamKeyboard = Keyboard(keyboardButtons, oneTimeKeyboard = true)
        }
    }

    object Conversation {
        private val YesNoKeyboard = Telegram.Keyboard(Seq(Seq(TextYes, TextNo)),
            oneTimeKeyboard = true)
    }

    case object OnDecide extends SettingsState

    case object OnProceed extends SettingsState

    case object OnLocation extends SettingsState

    case object OnLanguage extends SettingsState

    case object IsWebcamNeeded extends SettingsState

    case object OnWebcam extends SettingsState

    case object OnEnd extends SettingsState

    object NormalExit

}
