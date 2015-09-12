package com.github.raymank26.actor

/**
 * @author Anton Ermak
 */
trait Utils {

    def messageNotSupported(msg: Any): Unit =
        throw new IllegalStateException(s"no such handler for message $msg")
}
