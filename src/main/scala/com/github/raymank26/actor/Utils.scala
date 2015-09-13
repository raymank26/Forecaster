package com.github.raymank26.actor

import akka.event.LoggingAdapter

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Anton Ermak
 */
trait Utils {

    def messageNotSupported(msg: Any): Unit =
        throw new IllegalStateException(s"no such handler for message $msg")

    def runAsFuture[T](log: LoggingAdapter)(body: => T)(implicit ex: ExecutionContext): Future[T] = {
        val f = Future(body)
        f.onFailure {
            case exception => log.error(exception, "exception")
        }
        f
    }

}
