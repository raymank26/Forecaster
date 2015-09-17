package com.github.raymank26.actor

import akka.actor.ActorLogging

import scala.concurrent.{ExecutionContext, Future}

/**
 * Handy methods for.
 *
 * @author Anton Ermak
 */
private[actor] trait Utils {
    this: ActorLogging =>

    def messageIsNotSupported(msg: Any): Unit =
        throw new IllegalStateException(s"no such handler for message $msg")

    def runAsFuture[T](body: () => T)(implicit ex: ExecutionContext): Future[T] = {
        val f = Future(body())
        f.onFailure {
            case exception =>
                log.error(exception, "exception")
        }
        f
    }
}
