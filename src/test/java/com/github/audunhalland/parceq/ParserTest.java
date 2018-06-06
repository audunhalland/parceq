package com.github.audunhalland.parceq;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import com.github.audunhalland.parceq.Parser.Expression;
import com.github.audunhalland.parceq.Parser.Operator;
import com.github.audunhalland.parceq.Token.Type;
import io.vavr.collection.List;
import org.junit.Test;

public class ParserTest {
  private static final Token PREFIX_AND = new Token(Type.PREFIX_AND, "+");
  private static final Token PREFIX_ANDNOT = new Token(Type.PREFIX_ANDNOT, "-");
  private static final Token EOF = new Token(Type.EOF, "");

  private static Token token(String phrase) {
    return new Token(Token.Type.PHRASE, phrase);
  }

  private static Expression parse(Token ... tokens) {
    return new Parser().parse(List.of(tokens)).get();
  }

  private static Expression phrase(String phrase) {
    return Expression.of(phrase);
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
  public void parses_single_phrase() {
    assertThat(parse(
        token("foo"), EOF),
        equalTo(phrase("foo")));
  }

  @Test
  public void parses_two_phrases() {
    assertThat(parse(
        token("foo"), token("bar"), EOF),
        equalTo(
            or(
                phrase("foo"),
                phrase("bar"))));
  }

  @Test
  public void parses_prefix_and_operator() {
    assertThat(parse(
        token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                phrase("baz"),
                or(
                    phrase("foo"),
                    phrase("bar")))));
  }

  @Test
  public void parses_prefix_andnot() {
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(phrase("bar")),
                or(
                    phrase("foo"),
                    phrase("baz")))));
  }

  @Test
  public void parses_prefix_andnot_initially() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(phrase("foo")),
                or(
                    phrase("bar"),
                    phrase("baz")))));
  }

  @Test
  public void parses_multiple_prefix_operators() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                phrase("baz"),
                not(phrase("foo")),
                or(phrase("bar")))));
  }

  @Test
  public void parses_meaninglessly_successive_prefix_operators_leniently() {
    assertThat(parse(
        token("foo"), PREFIX_AND, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(
            and(
                not(phrase("bar")),
                or(phrase("foo")))));
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, PREFIX_AND, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(phrase("bar")),
                or(
                    phrase("foo"),
                    phrase("baz")))));
    assertThat("carelessly catenating prefix ANDNOTs equal out, but including excess PREFIX_ANDs has not effect",
        parse(PREFIX_AND, PREFIX_ANDNOT, PREFIX_AND, PREFIX_ANDNOT, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(and(not(phrase("bar")))));
  }
}
