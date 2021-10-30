package com.github.frroliveira

import com.github.frroliveira.Fake._
import org.scalatest.{MustMatchers, WordSpec}

class Fake2Spec extends WordSpec with MustMatchers {

  val intValue: Int = 1
  val booleanValue: Boolean = true

  "Fake[T]" should {
    "be cumulative" in {
      trait MultipleMethods {
        def method1: Int
      }
      val fake = Fake[MultipleMethods].fake(method1 = 1)
      val fakeW = fake.fake
      val fake2 = fakeW(method1 = intValue)

      fake2.method1 mustBe intValue
    }
  }
}
