package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._

class BetaReductionSpec extends Spec {

  "simplifies the ast by applying functons" - {
    "tuple field" in {
      val ast: Ast = Property(Tuple(List(Ident("a"))), "_1")
      BetaReduction(ast) mustEqual Ident("a")
    }
    "function apply" in {
      val function = Function(List(Ident("a")), Ident("a"))
      val ast: Ast = FunctionApply(function, List(Ident("b")))
      BetaReduction(ast) mustEqual Ident("b")
    }
  }

  "replaces identifiers by actuals" - {
    "ident" in {
      val ast: Ast = Ident("a")
      BetaReduction(ast, Ident("a") -> Ident("a'")) mustEqual
        Ident("a'")
    }
    "with inline" - {
      val entity = Entity("a", Nil)
      val (a, b, c, d) = (Ident("a"), Ident("b"), Ident("c"), Ident("d"))
      val (c1, c2, c3) = (Constant(1), Constant(2), Constant(3))

      "top level block" in {
        val block = Block(List(
          Val(a, entity),
          Val(b, a),
          Map(b, d, c1)
        ))
        BetaReduction(block) mustEqual Map(entity, d, c1)
      }
      "nested blocks" in {
        val inner = Block(List(
          Val(a, entity),
          Val(b, c2),
          Val(c, c3),
          Tuple(List(a, b, c))
        ))
        val outer = Block(List(
          Val(a, inner),
          Val(b, a),
          Val(c, b),
          c
        ))
        BetaReduction(outer) mustEqual Tuple(List(entity, c2, c3))
      }
    }
    "avoids replacing idents of an outer scope" - {
      "filter" in {
        val ast: Ast = Filter(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "map" in {
        val ast: Ast = Map(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "flatMap" in {
        val ast: Ast = FlatMap(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "concatMap" in {
        val ast: Ast = ConcatMap(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "sortBy" in {
        val ast: Ast = SortBy(Ident("a"), Ident("b"), Ident("b"), AscNullsFirst)
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "groupBy" in {
        val ast: Ast = GroupBy(Ident("a"), Ident("b"), Ident("b"))
        BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
      }
      "outer join" in {
        val ast: Ast = Join(LeftJoin, Ident("a"), Ident("b"), Ident("c"), Ident("d"), Tuple(List(Ident("c"), Ident("d"))))
        BetaReduction(ast, Ident("c") -> Ident("c'"), Ident("d") -> Ident("d'")) mustEqual ast
      }
      "option operation" - {
        "map" in {
          val ast: Ast = OptionMap(Ident("a"), Ident("b"), Ident("b"))
          BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
        }
        "forall" in {
          val ast: Ast = OptionForall(Ident("a"), Ident("b"), Ident("b"))
          BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
        }
        "exists" in {
          val ast: Ast = OptionExists(Ident("a"), Ident("b"), Ident("b"))
          BetaReduction(ast, Ident("b") -> Ident("b'")) mustEqual ast
        }
      }
    }
  }

  "doesn't shadow identifiers" - {
    "function apply" in {
      val ast: Ast = FunctionApply(Function(List(Ident("a"), Ident("b")), BinaryOperation(Ident("a"), NumericOperator.`/`, Ident("b"))), List(Ident("b"), Ident("a")))
      BetaReduction(ast) mustEqual BinaryOperation(Ident("b"), NumericOperator.`/`, Ident("a"))
    }
    "nested function apply" in {
      val f1 = Function(List(Ident("b")), BinaryOperation(Ident("a"), NumericOperator.`/`, Ident("b")))
      val f2 = Function(List(Ident("a")), f1)
      val ast: Ast = FunctionApply(FunctionApply(f2, List(Ident("b"))), List(Ident("a")))
      BetaReduction(ast) mustEqual BinaryOperation(Ident("b"), NumericOperator.`/`, Ident("a"))
    }
  }

  "deduplicates aliases in secondary table join" in {
    val aliases = List(Ident("x"), Ident("x"))
    val ast: Ast = Property(Tuple(aliases), "field")
    BetaReduction(ast, Ident("x") -> Tuple(aliases)) mustEqual ast
  }

  "reapplies the beta reduction if the structure changes" in {
    val ast: Ast = Property(Ident("a"), "_1")
    BetaReduction(ast, Ident("a") -> Tuple(List(Ident("a'")))) mustEqual
      Ident("a'")
  }

  "applies reduction only once" in {
    val ast: Ast = Ident("a")
    BetaReduction(ast, Ident("a") -> Ident("b"), Ident("b") -> Ident("c")) mustEqual
      Ident("b")
  }
}
