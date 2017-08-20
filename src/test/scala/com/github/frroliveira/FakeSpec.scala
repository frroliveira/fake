package com.github.frroliveira

import com.github.frroliveira.test._
import org.scalatest.{MustMatchers, WordSpec}

class FakeSpec extends WordSpec with MustMatchers {

  val intValue: Int = 1
  val booleanValue: Boolean = true
  val stringValue: String = "string"
  val doubleValue: Double = 2.0

  trait Trait {
    def method(arg: Boolean): Int
  }

  "Fake.apply[T]" should {
    "compile" when {
      "T is a trait" in {
        Fake[Trait]
      }
      "T is an abstract class" in {
        abstract class AbstractClass { def method: Int }
        // TODO: Fake[AbstractClass]
      }
      "T is located in another package" in {
        Fake[TraitWithoutParameters]
        Fake[test.TraitWithoutParameters]
        Fake[com.github.frroliveira.test.TraitWithoutParameters]
      }
    }

    "not compile" when {
      "T is a case class" in {
        case class CaseClass(value: Int)
        "Fake[CaseClass]" mustNot compile
      }
      "T is a non-abstract class" in {
        class NonAbstractClass(value: Int)
        "Fake[NonAbstractClass]" mustNot compile
      }
      "T is sealed" in {
        sealed trait SealedTrait extends Product with Serializable {
          val message: String
        }
        // TODO: "Fake[SealedTrait]" mustNot compile

        sealed abstract class SealedClass extends Product with Serializable {
          val message: String
        }
        "Fake[SealedClass]" mustNot compile
      }
      "T is final" in {
        final abstract class FinalClass { def method: Int = intValue }
        "Fake[FinalTrait]" mustNot compile
      }
    }

    "return an instance of T that overrides all unimplemented methods with ???" when {

      def assertUnimplemented[A](a: ⇒ A) = assertThrows[NotImplementedError](a)

      "T has both implemented and unimplemented methods" in {
        trait PartiallyImplemented {
          def implemented: Int = intValue
          def unimplemented1: Int
          def unimplemented2: Boolean
        }
        val fake = Fake[PartiallyImplemented]
        fake.implemented mustBe intValue
        assertUnimplemented(fake.unimplemented1)
        assertUnimplemented(fake.unimplemented2)
      }
      "T has no type parameters" in {
        val fake = Fake[TraitWithoutParameters]
        assertUnimplemented(fake.method)
      }
      "T has concrete type parameters" in {
        val fake = Fake[TraitWithSimpleParameters[Int, Boolean]]
        assertUnimplemented(fake.method(intValue))
      }
      "T has generic type parameters" in {
        trait GenericTrait[A] {
          type B
          def a: A

          private val fake = Fake[TraitWithSimpleParameters[A, B]]
          assertUnimplemented(fake.method(a))
        }
        new GenericTrait[Int] {
          type B = Boolean
          val a: Int = intValue
        }
      }
      "T has type aliases as type parameters" in {
        type A = Int
        type B = List[A]
        val fake = Fake[TraitWithSimpleParameters[A, B]]
        assertUnimplemented(fake.method(intValue))
      }
      "T has an existential type parameter" in {
        val fake = Fake[TraitWithExistentialType[Option, Int]]
        assertUnimplemented(fake.method(intValue))
      }
      "method has no arguments" in {
        val fake = Fake[MethodWithoutArguments[Int]]
        assertUnimplemented(fake.noArguments)
      }
      "method has arguments" in {
        val fake = Fake[MethodWithArguments[Int, Boolean, String]]
        assertUnimplemented(fake.arguments(intValue, booleanValue))
      }
      "method has concrete type parameters" in {
        val fake = Fake[MethodWithTypeParameters[Int]]
        assertUnimplemented(fake.typeParameters[Boolean, String](booleanValue, intValue))
      }
      "method has generic type parameters" in {
        // TODO:
      }
      "method has type aliases as type parameters" in {
        // TODO:
      }
      "method requires implicit values" in {
        val fake = Fake[MethodWithImplicits[Int, Boolean, String, Double]]
        implicit val implicitBoolean: Boolean = booleanValue
        implicit val implicitString: String = stringValue
        assertUnimplemented(fake.implicits(intValue))
      }
      "method requires context bounds" in {
        // TODO:
      }
      "method uses a nested type" in {
        val fake = Fake[MethodWithNestedType[Option, Int]]
        assertUnimplemented(fake.nested(Some(Some(intValue))))
      }
    }
  }

  "Fake[T].applyDynamic" should {
    "not compile" when {
      "dynamic method is invalid" in {
        "Fake[Trait].withMethod(1)" mustNot compile
        "Fake[Trait].fakemethod(1)" mustNot compile
      }
      "argument type doesn't match method return type" in {
        "Fake[Trait].fakeMethod(true)" mustNot typeCheck
      }
    }

    "compile" in {
      "Fake[Trait].fakeMethod(1)" must compile
    }
  }

  "Fake[T].applyDynamicNamed" should {
    "not compile" when {
      "dynamic method is invalid" in {
        // TODO: "Fake[Trait].fakeMethod(method = 1)" mustNot compile
      }
      "argument type doesn't match method return type" in {
        "Fake[Trait].fake(method = true)" mustNot typeCheck
        // TODO: function typechecking
      }
      "named argument doesn't match any method" in {
        // TODO: "Fake[Trait].fake(otherMethod = 1)" mustNot compile
      }
    }

    "compile" when {
      "dynamic method is 'apply'" in {
        "Fake[Trait](method = 1)" must compile
      }
      "dynamic method is 'fake'" in {
        "Fake[Trait].fake(method = 1)" must compile
      }
    }
  }

  "Fake[T]" should {
    "be immutable" in {
      val fake = Fake[Trait]
      val fake1 = fake.fake(method = intValue)
      val fake2 = fake.fake(method = intValue + 1)

      fake1.method(booleanValue) mustBe intValue
      fake2.method(booleanValue) mustBe intValue + 1
    }
    "be cumulative" in {
      trait MultipleMethods {
        def method1: Int
        def method2: Boolean
      }
      val fake = Fake[MultipleMethods]
        .fake(method1 = intValue)
        .fake(method2 = booleanValue)

      fake.method1 mustBe intValue
      fake.method2 mustBe booleanValue
    }
    "override duplicate method fake" in {
      Fake[Trait]
        .fake(method = intValue)
        .fake(method = intValue + 1)
        .method(booleanValue) mustBe intValue + 1
    }
  }

  "Fake[T] arguments" should {
    "accept constant values" when {
      "T has no type parameters" in {
        Fake[TraitWithoutParameters]
        // TODO:
      }
      "T has concrete type parameters" in {
        Fake[TraitWithSimpleParameters[Int, Boolean]]
          .fake(method = booleanValue)
          .method(intValue) mustBe booleanValue
      }
      "T has generic type parameters" in {
        trait GenericTrait[A] {
          type B
          def a: A
          def b: B

          Fake[TraitWithSimpleParameters[A, B]]
            .fake(method = b)
            .method(a) mustBe b
        }
        new GenericTrait[Int] {
          type B = Boolean
          val a: Int = intValue
          val b: Boolean = booleanValue
        }
      }
      "T has type aliases as type parameters" in {
        type A = Int
        type B = List[A]
        Fake[TraitWithSimpleParameters[A, B]]
          .fake(method = List(intValue))
          .method(intValue) mustBe List(intValue)
      }
      "T has an existential type parameter" in {
        Fake[TraitWithExistentialType[Option, Int]]
          .fake(method = Some(intValue))
          .method(intValue) mustBe Some(intValue)
      }
      "method has no arguments" in {
        Fake[MethodWithoutArguments[Int]]
          .fake(noArguments = intValue)
          .noArguments mustBe intValue
      }
      "method has arguments" in {
        Fake[MethodWithArguments[Int, Boolean, String]]
          .fake(arguments = stringValue)
          .arguments(intValue, booleanValue) mustBe stringValue
      }
      "method has concrete type parameters" in {
        // TODO:
      }
      "method has generic type parameters" in {
        // TODO:
      }
      "method has type aliases as type parameters" in {
        // TODO:
      }
      "method requires implicit values" in {
        implicit val implicitBoolean: Boolean = booleanValue
        implicit val implicitString: String = stringValue

        Fake[MethodWithImplicits[Int, Boolean, String, Double]]
          .fake(implicits = doubleValue)
          .implicits(intValue) mustBe doubleValue
      }
      "method requires context bounds" in {
        // TODO:
      }
      "method uses a nested type" in {
        Fake[MethodWithNestedType[Option, Int]]
          .fake(nested = intValue)
          .nested(Some(Some(intValue))) mustBe intValue
      }
    }
    "accept inline functions" when {
      "no variable outside of function scope is referenced" in {
        Fake[Trait]
          .fake(method = (b: Boolean) ⇒ if (b) 1 else 0)
          .method(true) mustBe 1
      }
      "variables outside of function scope are referenced" in {
        val outOfFunctionScope = intValue
        Fake[Trait]
          .fake(method = (_: Boolean) ⇒ outOfFunctionScope)
          .method(booleanValue) mustBe outOfFunctionScope
      }
    }
    "accept function references" in {
      val fn = (b: Boolean) ⇒ if (b) 1 else 0
      Fake[Trait]
        .fake(method = fn)
        .method(false) mustBe 0
    }
  }
}

