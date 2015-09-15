package com.github.raymank26.actor

import com.github.raymank26.actor.MessageDispatcher.SettingsSaved
import com.github.raymank26.actor.SettingsFSM.{Data, OnEnd, OnHello, OnLocation, OnWebcam, SavingStates, Talker}
import com.github.raymank26.controller.Forecast.GeoPrefs
import com.github.raymank26.controller.Telegram.Keyboard
import com.github.raymank26.controller.{Forecast, Telegram, Webcams}
import com.github.raymank26.db.Database
import com.github.raymank26.model.telegram.TelegramMessage
import com.github.raymank26.model.telegram.TelegramMessage.{Location, Text}
import com.github.raymank26.model.webcams.WebcamPreviewList

import akka.actor.FSM.Normal
import akka.actor.{Actor, ActorContext, FSM, Props}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * @author Anton Ermak.
 */
class SettingsFSM(chatId: Int) extends Actor with FSM[SavingStates, Data] {

    private val conversation = new Talker(chatId)
    private val parent = sender()
    private var webcams: WebcamPreviewList = _

    startWith(OnHello, new Data())

    conversation.sayHello()

    // waiting for location
    when(OnHello) {
        case Event(msg: TelegramMessage, data) if msg.isLocation =>
            val location = msg.content.asInstanceOf[Location]
            data.geo = GeoPrefs(location.latitude, location.longitude)
            goto(OnLocation).using(data)
        case _ => repeat()
    }

    // waiting for language
    when(OnLocation) {
        case Event(msg: TelegramMessage, data) if getLanguage(msg).isDefined =>
            data.language = getLanguage(msg).get
            goto(OnWebcam).using(data)
        case _ => repeat()
    }

    // waiting for webcam identifier
    when(OnWebcam) {
        case Event(msg: TelegramMessage, data) =>
            getWebcamIdentifier(msg) match {
                case Right(None) =>
                    self ! "Stop"
                    goto(OnEnd)
                case Right(Some(num)) =>
                    if (num < webcams.webcams.length) {
                        data.webcams += webcams.webcams(num).id
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
        case Event("Stop", data) =>
            parent ! SettingsSaved(chatId)
            Database.saveSettings(data)
            stop(Normal)
    }

    onTransition {
        case OnHello -> OnLocation => conversation.sayLanguage()
        case OnLocation -> OnWebcam =>
            webcams = SettingsFSM.loadWebcams(stateData.geo)
            conversation.sayWebcam(webcams)
        case OnWebcam -> OnEnd =>
            conversation.sayGoodbye()
        // remove actor. Notify watcher
        case from -> to if from == to => conversation.sayRetry(from)
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

    def apply(chatId: Int, context: ActorContext) = {
        context.actorOf(Props(classOf[SettingsFSM], chatId))
    }

    private def loadWebcams(geo: GeoPrefs): WebcamPreviewList = Webcams.getLinks(geo)

    sealed trait SavingStates

    case class Data() {
        var language: String = _
        var geo: Forecast.GeoPrefs = _
        var webcams: ArrayBuffer[String] = new ArrayBuffer()
    }

    private class Talker(chatId: Int) {

        def sayHello(): Unit =
            Telegram.sendMessage("Hi! Let's send me your location settings", chatId)

        def sayRetry(state: SavingStates): Unit = {
            Telegram.sendMessage("Try another one", chatId)
        }

        def sayLanguage(): Unit =
            Telegram.sendMessage("The next is language", chatId,
                replyKeyboard = Keyboard(buttons = Seq(Seq("en", "ru")),
                    oneTimeKeyboard = true))

        def sayWebcam(webcams: WebcamPreviewList): Unit = {
            val len = webcams.webcams.length
            val keyboardButtons = Seq
                .iterate(0, len) { _ + 1 }
                .map(_.toString)
                .:+("Stop")
                .grouped(len / 2)
                .toSeq
            Telegram.sendWebcamPreviews(webcams, chatId)
            Telegram.sendMessage("Which one?", chatId,
                replyKeyboard = Keyboard(keyboardButtons, oneTimeKeyboard = false))
        }

        def sayGoodbye(): Unit = {
            Telegram.sendMessage("Saved! Try to use /current", chatId)
        }
    }

    case object OnHello extends SavingStates

    case object OnLocation extends SavingStates

    case object OnLanguage extends SavingStates

    case object OnWebcam extends SavingStates

    case object OnEnd extends SavingStates

}
