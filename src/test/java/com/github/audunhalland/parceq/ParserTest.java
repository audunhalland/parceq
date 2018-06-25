package com.github.audunhalland.parceq;

import static com.github.audunhalland.parceq.ExpressionTestUtil.and;
import static com.github.audunhalland.parceq.ExpressionTestUtil.boost;
import static com.github.audunhalland.parceq.ExpressionTestUtil.not;
import static com.github.audunhalland.parceq.ExpressionTestUtil.or;
import static com.github.audunhalland.parceq.ExpressionTestUtil.term;
import static com.github.audunhalland.parceq.ExpressionTestUtil.termExpr;
import static com.github.audunhalland.parceq.ExpressionTestUtil.termsExpr;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.github.audunhalland.parceq.Token.Type;
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
    return new Parser(new TermAllocator()).parse(Stream.of(tokens));
  }

  @Test
  public void parses_single_term() {
    assertThat(parse(
        token("foo"), EOF),
        equalTo(boost(termExpr(0, "foo"))));
  }

  @Test
  public void parses_two_terms() {
    assertThat(parse(
        token("foo"), token("bar"), EOF),
        equalTo(boost(termsExpr(term(0, "foo"), term(1, "bar")))));
  }

  @Test
  public void parses_prefix_and_operator() {
    assertThat(parse(
        token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                termExpr(2, "baz"),
                boost(
                    termsExpr(
                        term(0, "foo"), term(1, "bar"))))));
  }

  @Test
  public void parses_prefix_andnot() {
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(termExpr(1, "bar")),
                boost(
                    termExpr(0, "foo"),
                    termExpr(2, "baz")))));
  }

  @Test
  public void parses_prefix_andnot_initially() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(termExpr(0, "foo")),
                boost(termsExpr(term(1, "bar"), term(2, "baz"))))));
  }

  @Test
  public void parses_multiple_prefix_operators() {
    assertThat(parse(
        PREFIX_ANDNOT, token("foo"), token("bar"), PREFIX_AND, token("baz"), EOF),
        equalTo(
            and(
                termExpr(2, "baz"),
                not(termExpr(0, "foo")),
                boost(termExpr(1, "bar")))));
  }

  @Test
  public void parses_meaninglessly_successive_prefix_operators_leniently() {
    assertThat(parse(
        token("foo"), PREFIX_AND, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(
            and(
                not(termExpr(1, "bar")),
                boost(termExpr(0, "foo")))));
    assertThat(parse(
        token("foo"), PREFIX_ANDNOT, PREFIX_AND, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                not(termExpr(1, "bar")),
                boost(
                    termExpr(0, "foo"),
                    termExpr(2, "baz")))));
    assertThat("carelessly catenating prefix ANDNOTs equal out, but including excess PREFIX_ANDs has not effect",
        parse(PREFIX_AND, PREFIX_ANDNOT, PREFIX_AND, PREFIX_ANDNOT, PREFIX_ANDNOT, token("bar"), EOF),
        equalTo(and(not(termExpr(0, "bar")))));
  }

  @Test
  public void infix_and_has_lower_precedence_and_no_boosting_of_and_arguments() {
    assertThat(parse(
        token("foo"), INFIX_AND, token("bar"), token("baz"), EOF),
        equalTo(
            and(
                termExpr(0, "foo"),
                termsExpr(term(1, "bar"), term(2, "baz")))));
  }

  @Test
  public void infix_or_is_distinct_from_no_operator() {
    assertThat(parse(
        token("foo"), INFIX_OR, token("bar"), token("baz"), EOF),
        equalTo(
            or(termExpr(0, "foo"),
            termsExpr(term(1, "bar"), term(2, "baz")))));
  }

  @Test
  public void infix_or_has_higher_precedence_than_and() {
    assertThat(parse(
        token("foo"), INFIX_OR, token("bar"), INFIX_AND, token("baz"), INFIX_OR, token("qux"), EOF),
        equalTo(
            and(
                or(termExpr(0, "foo"), termExpr(1, "bar")),
                or(termExpr(2, "baz"), termExpr(3, "qux")))));
  }

  @Test
  public void parentheses_control_precedence() {

  }
}
