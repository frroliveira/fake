package com.github.frroliveira

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

trait Fake[T] extends Dynamic { self: T =>

  val _fake: T = self

  def applyDynamic[U](dynamicMethod: String)(args: Any*): Fake[T] with T =
    macro FakeMacros.applyDynamicImpl[T]

  def applyDynamicNamed[U](dynamicMethod: String)(args: (String, Any)*): Fake[T] with T =
    macro FakeMacros.applyDynamicNamedImpl[T]
}

object Fake {
  def apply[T]: Fake[T] with T = macro FakeMacros.applyImpl[T]
}

@macrocompat.bundle
final class FakeMacros(val c: whitebox.Context) {
  import c.universe._

  case class Class(tpe: c.Type,
                   params: List[c.Type],
                   methods: List[Method])

  case class Method(name: c.TermName,
                    typeParams: List[TypeSymbol],
                    arguments: List[List[c.Symbol]],
                    returnType: c.Type)

  case class TypeParameterMap(resolved: Map[c.Symbol, TypeSymbol])

  import Generator._
  import Parser._

  def applyImpl[T: c.WeakTypeTag]: c.Tree = {
    val tpe = weakTypeOf[T]
    val cls = parseClass(tpe)
    val result = fakeTree(tpe, cls.methods.map(unimplementedMethodTree))
    result
  }

  def applyDynamicImpl[T: c.WeakTypeTag](dynamicMethod: c.Tree)(args: c.Tree*): c.Tree = {
    val tpe = weakTypeOf[T]
    val cls = parseClass(tpe)

    val fnStr = parseString(dynamicMethod)
    val targetMethod = cls.methods
      .find("fake" + _.name.toString.capitalize == fnStr)
      .getOrElse(c.abort(c.enclosingPosition, s"invalid member $fnStr"))

    val impl = cls.methods.map { m ⇒
      if (targetMethod == m) overriddenMethodTree(m, args.head)
      else overriddenMethodTree(m)
    }

    val result = withSelfTree(fakeTree(tpe, impl))
    result
  }

  def applyDynamicNamedImpl[T: c.WeakTypeTag](dynamicMethod: c.Tree)(args: c.Tree*): c.Tree = {
    val tpe = weakTypeOf[T]
    val cls = parseClass(tpe)

    val overrides = args.map { arg ⇒
      val q"(${name: c.Tree}, ${value: c.Tree})" = arg
      parseString(name) → value
    }.toMap

    val impl = cls.methods.map { m ⇒
      overrides.get(m.name.toString)
        .fold(overriddenMethodTree(m))(overriddenMethodTree(m, _))
    }

    withSelfTree(fakeTree(tpe, impl))
  }

  private object Parser {

    def parseString(tree: c.Tree): String = {
      val q"${str: String}" = tree
      str
    }

    def parseClass(tpe: c.Type): Class = {
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

    def parseMethod(methodSymbol: MethodSymbol, tpe: c.Type): Method = {
      Method(
        name = methodSymbol.name,
        typeParams = methodSymbol.infoIn(tpe).typeParams.map(_.asType),
        arguments = methodSymbol.infoIn(tpe).paramLists,
        returnType = methodSymbol.infoIn(tpe).finalResultType
      )
    }
  }

  private object Generator {
    import internal._

    def unimplementedMethodTree(method: Method): c.Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramss = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))

      q"$mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = ???"
    }

    def overriddenMethodTree(method: Method): c.Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramsValDefs = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))
      val paramsNames = paramsValDefs.map(_.map(_.name))

      q"""
      $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramsValDefs): ${method.returnType} = {
        self.${method.name}(...$paramsNames)
      }"""
    }

    def overriddenMethodTree(method: Method, body: c.Tree): c.Tree = {
      val mods = Modifiers(Flag.OVERRIDE)
      val paramss = method.arguments.map(_.map(s ⇒ setSymbol(internal.valDef(s), NoSymbol)))
      val paramsNames = paramss.map(_.map(_.name))

      if (body.tpe <:< method.returnType)
        q"""
        $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = {
          $body
        }"""
      else
        q"""
        $mods def ${method.name}[..${method.typeParams.map(typeDef(_))}](...$paramss): ${method.returnType} = {
          ${c.untypecheck(body)}(...$paramsNames)
        }"""
    }

    def withSelfTree(tree: c.Tree): c.Tree = {
      val selfSelect = Select(c.prefix.tree, TermName("_fake"))
      q""" {
        val self = $selfSelect
        $tree
      }"""
    }

    def fakeTree(tpe: c.Type, body: Iterable[c.Tree] = Nil): c.Tree =
      q"""
      new com.github.frroliveira.Fake[$tpe] with $tpe {
         ..$body
      }"""
  }
}
