//package io.getquill.lifting
//
//import org.scalatest.MustMatchers
//import org.scalatest.FreeSpec
//import scala.reflect.macros.blackbox.Context
//
//class LiftablesSpec extends FreeSpec with MustMatchers{
//  
//}
//
//case class Case(a: Int)
//case object Test
//
//class TestMacro(val c: Context) extends Liftables {
//  val universe = c.universe
//  def test = materializeLiftable[Case]
//}