package com.github.audunhalland.parceq;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public class Parser {

  // Pratt parser
  static class TopDownOperatorPrecedenceParser {
    private Token head;
    private Stream<Token> tail;

    public TopDownOperatorPrecedenceParser(Stream<Token> tokens) {
      this.head = tokens.head();
      this.tail = tokens.tail();
    }

    private Token next() {
      head = tail.head();
      tail = tail.tail();
      return head;
    }

    Option<Expression> parseExpression(int rightBindingPower) {
      if (tail.isEmpty()) {
        return Option.none();
      }

      Token current = this.head;
      next();
      Expression left = getNullDenotation(current);

      while (!tail.isEmpty() && rightBindingPower < this.head.getType().leftBindingPower) {
        current = this.head;
        next();
        left = getLeftDenotation(left, current);
      }

      return Option.of(left);
    }

    Option<Expression> parsePrefixArg() {
      if (!tail.isEmpty()) {
        final Token token = head;
        next();
        switch (token.getType()) {
          case PHRASE:
            return Option.of(getNullDenotation(token));
          case PREFIX_AND:
            return parsePrefixArg();
          case PREFIX_ANDNOT:
            return parsePrefixArg()
                .map(Expression::not);
          case LEFT_PAREN:
            // FIXME:
            return null;
          default:
            return Option.none();
        }
      }
      return Option.none();
    }

    Tuple2<Option<Expression>, Option<Expression>> parsePrefixArgAndRight(int rightBindingPower) {
      return new Tuple2<>(parsePrefixArg(), parseExpression(rightBindingPower));
    }

    private Expression getNullDenotation(Token token) {
      switch (token.getType()) {
        case PHRASE:
          return Expression.of(token.getValue());
        case PREFIX_AND:
          return parsePrefixArgAndRight(Token.Type.PREFIX_AND.leftBindingPower)
              .map2(optRight -> optRight.getOrElse(Expression.noop()))
              .apply((optArg, right) ->
                  optArg
                      .map(arg -> arg.and(right.toAndArg()))
                      .getOrElse(right));
        case PREFIX_ANDNOT:
          return parsePrefixArgAndRight(Token.Type.PREFIX_ANDNOT.leftBindingPower)
              .map2(optRight -> optRight.getOrElse(Expression.noop()))
              .apply((optArg, right) ->
                  optArg
                      .map(arg -> arg.not().and(right.toAndArg()))
                      .getOrElse(right));
        default:
          return Expression.noop();
      }
    }

    private Expression getLeftDenotation(Expression left, Token token) {
      switch (token.getType()) {
        case PHRASE:
          return left.or(Expression.of(token.getValue()));
        case INFIX_AND:
          return parseExpression(Type.INFIX_AND.leftBindingPower)
              .map(left::and)
              .getOrElse(left);
        case INFIX_OR:
          return parseExpression(Type.INFIX_OR.leftBindingPower)
              .map(left::or)
              .getOrElse(left);
        case PREFIX_AND:
          return parsePrefixArgAndRight(Token.Type.PREFIX_AND.leftBindingPower)
              .map2(optRight -> optRight.map(left::extend).getOrElse(left))
              .apply((optArg, rest) ->
                  optArg
                      .map(arg -> arg.and(rest.toAndArg()))
                      .getOrElse(rest));
        case PREFIX_ANDNOT:
          return parsePrefixArgAndRight(Token.Type.PREFIX_ANDNOT.leftBindingPower)
              .map2(optRight -> optRight.map(left::extend).getOrElse(left))
              .apply((optArg, rest) ->
                  optArg
                      .map(arg -> arg.not().and(rest.toAndArg()))
                      .getOrElse(rest));
        default:
          return left;
      }
    }
  }

  public Option<Expression> parse(Stream<Token> tokens) {
    final TopDownOperatorPrecedenceParser parser =
        new TopDownOperatorPrecedenceParser(tokens);

    return parser.parseExpression(0);
  }
}
