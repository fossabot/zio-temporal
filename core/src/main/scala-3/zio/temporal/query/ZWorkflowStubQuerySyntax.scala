package zio.temporal.query

import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}

import scala.quoted.*
import scala.reflect.ClassTag

trait ZWorkflowStubQuerySyntax {
  inline def query[R](inline f: R)(using ctg: ClassTag[R]): TemporalIO[TemporalClientError, R] =
    ${ ZWorkflowStubQuerySyntax.queryImpl[R]('f, 'ctg) }

  inline def query[E, R](inline f: Either[E, R])(using ctg: ClassTag[Either[E, R]]): TemporalIO[TemporalError[E], R] =
    ${ ZWorkflowStubQuerySyntax.queryEitherImpl[E, R]('f, 'ctg) }
}

object ZWorkflowStubQuerySyntax {
  def queryImpl[R: Type](
    f:       Expr[R],
    ctg:     Expr[ClassTag[R]]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalClientError, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val theQuery = buildQueryInvocation[R](betaReduceExpression(f).asTerm, ctg)

    '{
      _root_.zio.temporal.internal.TemporalInteraction.from[R] {
        ${ theQuery.asExprOf[R] }
      }
    }.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }

  def queryEitherImpl[E: Type, R: Type](
    f:       Expr[Either[E, R]],
    ctg:     Expr[ClassTag[Either[E, R]]]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalError[E], R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val theQuery = buildQueryInvocation[Either[E, R]](betaReduceExpression(f).asTerm, ctg)

    '{
      _root_.zio.temporal.internal.TemporalInteraction.fromEither[E, R] {
        ${
          theQuery.asExprOf[Either[E, R]]
        }
      }
    }.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }
}
