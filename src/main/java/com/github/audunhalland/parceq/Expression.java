package com.github.audunhalland.parceq;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.Objects;

public class Expression {
  private final Either<SubExpression, Term> value;

  Expression(Either<SubExpression, Term> value) {
    this.value = value;
  }

  public static Expression of(SubExpression subExpression) {
    return new Expression(Either.left(subExpression));
  }

  public static Expression of(Operator operator, List<Expression> operands) {
    return of(new SubExpression(operator, operands));
  }

  public static Expression of(Term term) {
    return new Expression(Either.right(term));
  }

  public static Expression noop() {
    return of(new SubExpression(Operator.NOOP, List.empty()));
  }

  public boolean isTerm() {
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
    if (isTerm()) {
      return of(Operator.OR, List.of(this));
    } else {
      return this;
    }
  }

  public Option<List<Term>> termsOnly() {
    if (isCompound()) {
      return value.getLeft().termsOnly();
    } else {
      return Option.of(List.of(value.get()));
    }
  }

  public Expression appendTerm(Term term) {
    if (isNoop()) {
      return of(term);
    }

    final Option<List<Term>> thisTermsOnly = this.termsOnly();
    if (thisTermsOnly.isDefined()) {
      return Expression.of(Operator.TERMS,
          thisTermsOnly.get()
              .append(term)
              .map(Expression::of));
    }
    return Expression.of(Operator.OR, List.of(this, Expression.of(term)));
  }

  public Expression and(Expression other) {
    if (isNoop()) {
      return other;
    } else if (isAnd()) {
      if (other.isAnd()) {
        return of(Operator.AND,
            value.getLeft().operands.appendAll(other.value.getLeft().operands));
      }
      return of(Operator.AND, value.getLeft().operands.append(other));
    } else if (isTerm() && other.isAnd()) {
      return of(Operator.AND, List.of(of(value.get())).appendAll(other.value.getLeft().operands));
    } else if (isAnd() && other.isTerm()) {
      return of(Operator.AND, value.getLeft().operands.append(of(other.value.get())));
    } else {
      return of(Operator.AND, List.of(this, other));
    }
  }

  public Expression or(Expression other) {
    if (isNoop()) {
      return other;
    } else if (isOr()) {
      if (other.isOr()) {
        return of(Operator.OR,
            value.getLeft().operands.appendAll(other.value.getLeft().operands));
      }
      return of(Operator.OR, value.getLeft().operands.append(other));
    } else if (isTerm() && other.isOr()) {
      return of(Operator.OR, List.of(of(value.get())).appendAll(other.value.getLeft().operands));
    } else if (isOr() && other.isTerm()) {
      return of(Operator.OR, value.getLeft().operands.append(of(other.value.get())));
    } else {
      return of(Operator.OR, List.of(this, other));
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
      return value.get().getValue();
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

    Option<List<Term>> termsOnly() {
      if (operator != Operator.TERMS) {
        return Option.none();
      }
      List<Term> terms = List.empty();
      for (Expression expr : operands) {
        Option<List<Term>> exprTerms = expr.termsOnly();
        if (!exprTerms.isDefined()) {
          return Option.none();
        }
        terms = terms.appendAll(exprTerms.get());
      }
      return Option.of(terms);
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
