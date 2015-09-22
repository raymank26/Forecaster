package com.github.raymank26

import org.scalatest.{FunSuite, Matchers}

import scala.io.Source

/**
 * @author Anton Ermak
 */
class Suite extends FunSuite with Matchers {

    protected def readFile(filename: String) = {
        Source.fromInputStream(getClass.getResourceAsStream(filename)).mkString
    }

}
