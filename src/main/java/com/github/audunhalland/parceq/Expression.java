package com.github.audunhalland.parceq;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.Objects;

public class Expression {
  private final Either<SubExpression, String> value;

  Expression(Either<SubExpression, String> value) {
    this.value = value;
  }

  public static Expression of(SubExpression subExpression) {
    return new Expression(Either.left(subExpression));
  }

  public static Expression of(Operator operator, List<Expression> operands) {
    return of(new SubExpression(operator, operands));
  }

  public static Expression of(String phrase) {
    return new Expression(Either.right(phrase));
  }

  public static Expression noop() {
    return of(new SubExpression(Operator.NOOP, List.empty()));
  }

  public boolean isPhrase() {
    return value.isRight();
  }

  public boolean isCompound() {
    return value.isLeft();
  }

  public boolean isAnd() {
    return isCompound() && value.getLeft().operator == Operator.AND;
  }

  public boolean isOr() {
    return isCompound() && value.getLeft().operator == Operator.OR;
  }

  public boolean isNot() {
    return isCompound() && value.getLeft().operator == Operator.NOT;
  }

  public boolean isNoop() {
    return isCompound() && value.getLeft().operator == Operator.NOOP;
  }

  public Expression toAndArg() {
    if (isPhrase()) {
      return of(Operator.OR, List.of(this));
    } else {
      return this;
    }
  }

  public Option<List<String>> orPhrasesOnly() {
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
}
