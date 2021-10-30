package com.github.frroliveira

import scala.reflect.macros.whitebox

trait Fake[T] {
  def fake: FakeFrom[T]
}

final class FakeEmpty[T] extends Dynamic {

  def applyDynamicNamed[U](dynamicMethod: String)(args: (String, Any)*): Fake[T] =
    macro FakeMacros.applyDynamicNamedImpl[T]
}

final class FakeFrom[T](val value: T) extends Dynamic {

  def applyDynamicNamed[U](dynamicMethod: String)(args: (String, Any)*): Fake[T] =
    macro FakeMacros.applyDynamicNamedImpl[T]
}


object Fake {

  def apply[T]: FakeEmpty[T] = new FakeEmpty[T]

  def unsafe[T]: Fake[T] = macro FakeMacros.applyImpl[T]

  implicit def fakeToT[T](fake: Fake[T]): T = fake.fake.value
}

@macrocompat.bundle
final class FakeMacros(val c: whitebox.Context) {
  import c.universe._

  case class Class(tpe: Type,
                   params: List[Type],
                   methods: List[Method])

  case class Method(name: TermName,
                    typeParams: List[TypeSymbol],
                    arguments: List[List[Symbol]],
                    returnType: Type)

  case class TypeParameterMap(resolved: Map[Symbol, TypeSymbol])

  import Printer._
  import Parser._

  def applyImpl[T: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    val cls = parseClass(tpe)
    val result = fakeTree(tpe, cls.methods.map(unimplementedMethodTree))
    println(result)
    result
  }

  def applyDynamicNamedImpl[T: WeakTypeTag](dynamicMethod: Tree)(args: Tree*): Tree = {
    val tpe = weakTypeOf[T]
    val cls = parseClass(tpe)

    val method = parseString(dynamicMethod)
    if (!Set("apply").contains(method)) c.abort(c.enclosingPosition, "Invalid member")

    val overrides = args.map { arg ⇒
      val q"(${name: Tree}, ${value: Tree})" = arg
      parseString(name) → value
    }.toMap

    val impl = cls.methods.map { m ⇒
      overrides.get(m.name.toString)
        .fold(overriddenMethodTree(m))(overriddenMethodTree(m, _))
    }

    val result = overrideFakeTree(tpe, impl)
    println(result)
    result
  }

  private object Parser {

    def parseString(tree: Tree): String = {
      val q"${str: String}" = tree
      str
    }

    def parseClass(tpe: Type): Class = {
      Class(
        tpe,
        tpe.typeArgs,
        tpe.members
          .filter(_.isAbstract)
          .collect { case symbol: MethodSymbol ⇒ symbol }
          .map(parseMethod(_, tpe))
          .toList
      )
    }

    def parseMethod(methodSymbol: MethodSymbol, tpe: Type): Method = {
      Method(
        name = methodSymbol.name,
        typeParams = methodSymbol.infoIn(tpe).typeParams.map(_.asType),
        arguments = methodSymbol.infoIn(tpe).paramLists,
        returnType = methodSymbol.infoIn(tpe).finalResultType
      )
    }

    def parseFakeMethods(tree: Tree): List[scala.reflect.api.Trees#Tree] = {
      val q"new ..$_ { val _value: $_ = new ..$_ { ..$methods }  }" = tree
      methods.toList
    }
  }

  private object Printer {
    import internal._

    def unimplementedMethodTree(method: Method): Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramss = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))

      q"$mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = ???"
    }

    def overriddenMethodTree(method: Method): Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramsValDefs = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))
      val paramsNames = paramsValDefs.map(_.map(_.name))

      q"""
      $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramsValDefs): ${method.returnType} = {
        self.${method.name}(...$paramsNames)
      }"""
    }

    def overriddenMethodTree(method: Method, body: Tree): Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramss = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))
      val paramsNames = paramss.map(_.map(_.name))

      if (body.tpe <:< method.returnType)
        q"""
        $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = {
          ${c.untypecheck(body)}
        }"""
      else
        q"""
        $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = {
          ${c.untypecheck(body)}(...$paramsNames)
        }"""
    }

    def overrideFakeTree(tpe: Type, body: Iterable[Tree] = Nil): Tree = {
      if (body.size == 1) fakeTree(tpe, body)
      else {
        val tree = fakeTree(tpe, body)
        val selfSelect = Select(c.prefix.tree, TermName("value"))
        q""" {
          val self = $selfSelect
          $tree
        }"""
      }
    }

    def fakeTree(tpe: Type, body: Iterable[Tree] = Nil): Tree = {
      q"""
      new com.github.frroliveira.Fake[$tpe] {
        def fake: com.github.frroliveira.FakeFrom[$tpe] =
          new com.github.frroliveira.FakeFrom[$tpe](
            new $tpe { ..$body }
          )
      }"""
    }
  }
}