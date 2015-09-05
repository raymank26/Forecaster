package com.github.raymank26.actor

import akka.actor.Actor

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
class MessageUpdater extends Actor {

    private var lastOffset: Int = ???

    override def receive: Receive = {
        case _ =>
    }
}
