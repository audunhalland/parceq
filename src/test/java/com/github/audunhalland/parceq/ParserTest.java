package com.github.audunhalland.parceq;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.junit.Test;

public class ParserTest {
  private static final Token PREFIX_AND = new Token(Type.PREFIX_AND, "+");
  private static final Token PREFIX_ANDNOT = new Token(Type.PREFIX_ANDNOT, "-");
  private static final Token INFIX_AND = new Token(Type.INFIX_AND, "&&");
  private static final Token INFIX_OR = new Token(Type.INFIX_OR, "||");
  private static final Token EOF = new Token(Type.EOF, "");

  private static Token token(String value) {
    return new Token(Token.Type.WORD, value);
  }

  private static Expression parse(Token ... tokens) {
    return new Parser().parse(Stream.of(tokens)).get();
  }

  private static Expression term(String value) {
    return Expression.of(new Term(value));
  }

  private static Expression terms(String ... terms) {
    return List.of(terms)
        .foldLeft(Expression.noop(),
            (expr, term) -> expr.appendTerm(new Term(term)));
  }

  private static Expression and(Expression ... expressions) {
    return Expression.of(Operator.AND, List.of(expressions));
  }

  private static Expression or(Expression ... expressions) {
    return Expression.of(Operator.OR, List.of(expressions));
  }

  private static Expression not(Expression expression) {
    return Expression.of(Operator.NOT, List.of(expression));
  }

  @Test
  public void parses_single_term() {
    assertThat(parse(
        token("foo"), EOF),
        equalTo(term("foo")));
  }

  @Test
  public void parses_two_terms() {
    assertThat(parse(
        token("foo"), token("bar"), EOF),
        equalTo(terms("foo", "bar")));
  }

  @Test
  public void parses_prefix_and_operator() {
    assertThat(parse(
        token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                term("baz"),
                terms("foo", "bar"))));
  }

  @Test
  public void parses_prefix_andnot() {
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(term("bar")),
                or(
                    term("foo"),
                    term("baz")))));
  }

  @Test
  public void parses_prefix_andnot_initially() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(term("foo")),
                terms("bar", "baz"))));
  }

  @Test
  public void parses_multiple_prefix_operators() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                term("baz"),
                not(term("foo")),
                or(term("bar")))));
  }

  @Test
  public void parses_meaninglessly_successive_prefix_operators_leniently() {
    assertThat(parse(
        token("foo"), PREFIX_AND, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(
            and(
                not(term("bar")),
                or(term("foo")))));
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, PREFIX_AND, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(term("bar")),
                or(
                    term("foo"),
                    term("baz")))));
    assertThat("carelessly catenating prefix ANDNOTs equal out, but including excess PREFIX_ANDs has not effect",
        parse(PREFIX_AND, PREFIX_ANDNOT, PREFIX_AND, PREFIX_ANDNOT, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(and(not(term("bar")))));
  }

  @Test
  public void infix_and_has_lower_precedence() {
    assertThat(parse(
        token("foo"), INFIX_AND, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                term("foo"),
                terms("bar", "baz"))));
  }

  @Test
  public void infix_or_is_distinct_from_no_operator() {
    assertThat(parse(
        token("foo"), INFIX_OR, token("bar"), token("baz"), EOF),
        equalTo(
            or(term("foo"),
            terms("bar", "baz"))));
  }

  @Test
  public void infix_or_has_higher_precedence_than_and() {
    assertThat(parse(
        token("foo"), INFIX_OR, token("bar"), INFIX_AND, token("baz"), INFIX_OR, token("qux"), EOF),
        equalTo(
            and(
                or(term("foo"), term("bar")),
                or(term("baz"), term("qux")))));
  }

  @Test
  public void parentheses_control_precedence() {

  }
}
