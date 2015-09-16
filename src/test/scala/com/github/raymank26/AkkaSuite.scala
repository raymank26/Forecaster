package com.github.raymank26

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

/**
 * @author Anton Ermak
 */
class AkkaSuite extends TestKit(ActorSystem("system"))
                        with DefaultTimeout
                        with ImplicitSender
                        with FunSuiteLike
                        with Matchers
                        with BeforeAndAfterAll {

    override protected def afterAll(): Unit = {
        shutdown()
    }
}
