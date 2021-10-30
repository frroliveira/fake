package com.github.frroliveira.test

trait TraitWithoutParameters {
  def method: Int
}

trait TraitWithSimpleParameters[T, U] {
  def method(t: T): U
}

trait TraitWithExistentialType[F[_], T] {
  def method(t: T): F[T]
}

trait MethodWithoutArguments[T] {
  def noArguments: T
}

trait MethodWithArguments[T, U, V] {
  def arguments(arg1: T, arg2: U): V
}

trait MethodWithVarArgs[T, U, V] {
  def varargs(first: T, rest: (T, U)*): V
}

trait MethodWithTypeParameters[T] {
  def typeParameters[U, V](u: U, t: T): V
}

trait MethodWithImplicits[T, U, V, X] {
  def implicits(t: T)(implicit u: U, v: V): X
}

trait MethodWithContextBounds[F[_], T, U] {
  def contextBounds[V: F](v: V)(implicit t: T): U
}

trait MethodWithNestedType[F[_], T] {
  def nested(f: F[F[T]]): T
}
