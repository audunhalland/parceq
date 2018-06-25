package com.github.audunhalland.parceq;

import io.vavr.collection.List;

public class ExpressionTestUtil {
  public static Term term(int id, String value) {
    return new Term(id, value);
  }

  public static Expression termExpr(int id, String value) {
    return Expression.of(new Term(id, value));
  }

  public static Expression termsExpr(Term ... terms) {
    return List.of(terms)
        .foldLeft(Expression.noop(), Expression::appendTerm);
  }

  public static Expression and(Expression ... expressions) {
    return Expression.of(Operator.AND, List.of(expressions));
  }

  public static Expression or(Expression ... expressions) {
    return Expression.of(Operator.OR, List.of(expressions));
  }

  public static Expression not(Expression expression) {
    return Expression.of(Operator.NOT, List.of(expression));
  }

  public static Expression boost(Expression ... expressions) {
    return Expression.of(Operator.BOOST, List.of(expressions));
  }

}
