package io.getquill.lifting

import language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.Universe

private class LiftablesMaro(val c: Context) {
  import c.universe._

  def test = q"1"
  
  def materializeLiftable[T](implicit t: WeakTypeTag[T]): Tree = {
    t.tpe.typeSymbol match {
      case sym: ClassSymbol =>
        sym match {
          case sym if sym.isModuleClass =>
            q"""
              import ${c.prefix}.universe._
              Liftable[$t] {
                case _ => Ident(rootMirror.staticModule(${sym.module.fullName}))
              }
            """
          case sym if sym.isCaseClass =>
            val p = t.tpe.decl(nme.CONSTRUCTOR).asMethod.paramLists match {
              case params :: Nil => params.map(p => q"$p")
              case other => ???
            }
            q"""
              import ${c.prefix}.universe._
              Liftable[$t] {
                case ${sym.companion}(..$p) => Apply(Ident(rootMirror.staticModule(${sym.companion})), scala.List(..$p))
              }
            """
        }
      case other => ???
    }
  }
}

trait Liftables {
  val universe: Universe
  implicit def materializeLiftable[T]: universe.Liftable[T] = macro LiftablesMaro.materializeLiftable[T]
}