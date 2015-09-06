package com.github.raymank26

import akka.http.scaladsl.server.Directives._

/**
 * @author Anton Ermak
 */
trait Api {

    val routes = path("hello") {
        get {
            complete {
                "foo"
            }
        }
    }
}
