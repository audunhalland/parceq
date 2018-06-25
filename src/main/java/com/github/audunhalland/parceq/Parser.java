package com.github.audunhalland.parceq;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public class Parser {
  private final TermAllocator termAllocator;

  public Parser(TermAllocator termAllocator) {
    this.termAllocator = termAllocator;
  }

  // Pratt parser
  static class TopDownOperatorPrecedenceParser {
    private final TermAllocator termAllocator;
    private Token head;
    private Stream<Token> tail;

    public TopDownOperatorPrecedenceParser(TermAllocator termAllocator,
        Stream<Token> tokens) {
      this.termAllocator = termAllocator;
      this.head = tokens.head();
      this.tail = tokens.tail();
    }

    private Token next() {
      head = tail.head();
      tail = tail.tail();
      return head;
    }

    Expression parseExpression(int rightBindingPower) {
      if (tail.isEmpty()) {
        return Expression.noop();
      }

      Token current = this.head;
      next();
      Expression left = getNullDenotation(current);

      while (!tail.isEmpty() && rightBindingPower < this.head.getType().leftBindingPower) {
        current = this.head;
        next();
        left = getLeftDenotation(left, current);
      }

      return left;
    }

    Expression parsePrefixArg() {
      if (!tail.isEmpty()) {
        final Token token = head;
        next();
        switch (token.getType()) {
          case WORD:
            return getNullDenotation(token);
          case PREFIX_AND:
            return parsePrefixArg();
          case PREFIX_ANDNOT:
            return parsePrefixArg().not();
          case LEFT_PAREN:
            // FIXME:
            return null;
          default:
            return Expression.noop();
        }
      }

      return Expression.noop();
    }

    Tuple2<Expression, Expression> parsePrefixArgAndRight(int rightBindingPower) {
      return new Tuple2<>(parsePrefixArg(), parseExpression(rightBindingPower));
    }

    private Expression getNullDenotation(Token token) {
      switch (token.getType()) {
        case WORD:
          return Expression.of(termAllocator.createRootTerm(token.getValue()));
        case PREFIX_AND:
          return parsePrefixArgAndRight(Token.Type.PREFIX_AND.leftBindingPower)
              .apply((arg, right) -> arg.and(right.wrap()));
        case PREFIX_ANDNOT:
          return parsePrefixArgAndRight(Token.Type.PREFIX_ANDNOT.leftBindingPower)
              .apply((arg, right) -> arg.not().and(right.wrap()));
        default:
          return Expression.noop();
      }
    }

    private Expression getLeftDenotation(Expression left, Token token) {
      switch (token.getType()) {
        case WORD:
          return left.appendTerm(termAllocator.createRootTerm(token.getValue()));
        case INFIX_AND:
          return left.and(parseExpression(Type.INFIX_AND.leftBindingPower));
        case INFIX_OR:
          return left.or(parseExpression(Type.INFIX_OR.leftBindingPower));
        case PREFIX_AND:
          return parsePrefixArgAndRight(Token.Type.PREFIX_AND.leftBindingPower)
              .map2(left::extend)
              .apply((arg, rest) -> arg.and(rest.wrap()));
        case PREFIX_ANDNOT:
          return parsePrefixArgAndRight(Token.Type.PREFIX_ANDNOT.leftBindingPower)
              .map2(left::extend)
              .apply((arg, rest) -> arg.not().and(rest.wrap()));
        default:
          return left;
      }
    }
  }

  public Expression parse(Stream<Token> tokens) {
    final TopDownOperatorPrecedenceParser parser =
        new TopDownOperatorPrecedenceParser(termAllocator, tokens);

    return parser.parseExpression(0).wrap();
  }
}
