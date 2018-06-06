package com.github.audunhalland.parceq;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.Objects;

public class Parser {

  public enum Operator {
    NOOP,
    OR,
    AND,
    NOT,
  }

  public static class SubExpression {
    final Operator operator;
    final List<Expression> operands;

    public SubExpression(Operator operator, List<Expression> operands) {
      this.operator = operator;
      this.operands = operands
          .filter(expr -> !expr.isNoop());
    }

    Option<List<String>> orPhrasesOnly() {
      if (operator != Operator.OR) {
        return Option.none();
      }
      List<String> phrases = List.empty();
      for (Expression expr : operands) {
        Option<List<String>> exprPhrases = expr.orPhrasesOnly();
        if (!exprPhrases.isDefined()) {
          return Option.none();
        }
        phrases = phrases.appendAll(exprPhrases.get());
      }
      return Option.of(phrases);
    }

    @Override
    public String toString() {
      return operator
          + "("
          + String.join(", ", operands.map(Object::toString))
          + ")";
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      return other instanceof SubExpression
          && operator == ((SubExpression) other).operator
          && operands.equals(((SubExpression) other).operands);
    }

    @Override
    public int hashCode() {
      return Objects.hash(operator, operands);
    }
  }

  public static class Expression {
    final Either<SubExpression, String> value;

    Expression(Either<SubExpression, String> value) {
      this.value = value;
    }

    static Expression of(SubExpression subExpression) {
      return new Expression(Either.left(subExpression));
    }

    static Expression of(Operator operator, List<Expression> operands) {
      return of(new SubExpression(operator, operands));
    }

    static Expression of(String phrase) {
      return new Expression(Either.right(phrase));
    }

    static Expression noop() {
      return of(new SubExpression(Operator.NOOP, List.empty()));
    }

    boolean isPhrase() {
      return value.isRight();
    }

    boolean isCompound() {
      return value.isLeft();
    }

    boolean isAnd() {
      return isCompound() && value.getLeft().operator == Operator.AND;
    }

    boolean isOr() {
      return isCompound() && value.getLeft().operator == Operator.OR;
    }

    boolean isNot() {
      return isCompound() && value.getLeft().operator == Operator.NOT;
    }

    boolean isNoop() {
      return isCompound() && value.getLeft().operator == Operator.NOOP;
    }

    private Expression toAndArg() {
      if (isPhrase()) {
        return of(Operator.OR, List.of(this));
      } else {
        return this;
      }
    }

    Option<List<String>> orPhrasesOnly() {
      if (isCompound()) {
        return value.getLeft().orPhrasesOnly();
      } else {
        return Option.of(List.of(value.get()));
      }
    }

    public Expression or(Expression other) {
      Option<List<String>> thisPhrasesOnly = this.orPhrasesOnly();
      Option<List<String>> otherPhrasesOnly = other.orPhrasesOnly();
      if (thisPhrasesOnly.isDefined() && otherPhrasesOnly.isDefined()) {
        return Expression.of(Operator.OR,
            thisPhrasesOnly.get()
                .appendAll(otherPhrasesOnly.get())
                .map(Expression::of));
      }
      return Expression.of(Operator.OR, List.of(this, other));
    }

    public Expression and(Expression other) {
      if (isAnd()) {
        if (other.isAnd()) {
          return of(Operator.AND,
              value.getLeft().operands.appendAll(other.value.getLeft().operands));
        }
        return of(Operator.AND, value.getLeft().operands.append(other));
      } else if (isPhrase() && other.isAnd()) {
        return of(Operator.AND, List.of(of(value.get())).appendAll(other.value.getLeft().operands));
      } else if (isAnd() && other.isPhrase()) {
        return of(Operator.AND, value.getLeft().operands.append(of(other.value.get())));
      } else {
        return of(Operator.AND, List.of(this, other));
      }
    }

    public Expression not() {
      if (isCompound()
          && value.getLeft().operator == Operator.NOT
          && value.getLeft().operands.size() == 1) {
        return value.getLeft().operands.get(0);
      } else {
        return of(Operator.NOT, List.of(this));
      }
    }

    public Expression extend(Expression other) {
      if (isCompound()) {
        switch (value.getLeft().operator) {
          case OR:
            return this.or(other);
          case AND:
            return this.and(other);
          case NOOP:
          default:
            return other;
        }
      } else {
        return this.or(other);
      }
    }

    @Override
    public String toString() {
      if (isCompound()) {
        return value.getLeft().toString();
      } else {
        return value.get();
      }
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      return other instanceof Expression
          && value.equals(((Expression) other).value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  // Pratt parser
  static class TopDownOperatorPrecedenceParser {
    private Token head;
    private List<Token> tail;

    public TopDownOperatorPrecedenceParser(List<Token> tokens) {
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
            // FIXMRE:
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

  public Option<Expression> parse(List<Token> tokens) {
    final TopDownOperatorPrecedenceParser parser =
        new TopDownOperatorPrecedenceParser(tokens);

    return parser.parseExpression(0);
  }
}
