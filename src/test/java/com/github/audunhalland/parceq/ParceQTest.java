package com.github.audunhalland.parceq;

import static com.github.audunhalland.parceq.ExpressionTestUtil.and;
import static com.github.audunhalland.parceq.ExpressionTestUtil.boost;
import static com.github.audunhalland.parceq.ExpressionTestUtil.not;
import static com.github.audunhalland.parceq.ExpressionTestUtil.or;
import static com.github.audunhalland.parceq.ExpressionTestUtil.term;
import static com.github.audunhalland.parceq.ExpressionTestUtil.termExpr;
import static com.github.audunhalland.parceq.ExpressionTestUtil.termsExpr;
import static org.junit.Assert.*;
import static org.hamcrest.core.IsEqual.equalTo;

import io.vavr.collection.List;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.hamcrest.core.IsSame;
import org.junit.Test;

public class ParceQTest {
  @Test
  public void parse_of_valid_input_succeeds() {
    final Try<ParceQ> parceq = ParceQ.parse(
        new StringReader("foo"));
    assertThat(parceq.isSuccess(), equalTo(true));
  }

  @Test
  public void parse_of_failing_input_yields_io_exception() {
    final IOException error = new IOException();
    final Try<ParceQ> parceq = ParceQ.parse(
        new Reader() {
          @Override
          public int read(char[] cbuf, int off, int len) throws IOException {
            throw error;
          }

          @Override
          public void close() throws IOException {

          }
        });
    assertThat(parceq.getCause(), IsSame.sameInstance(error));
  }

  @Test
  public void generates_shingles() {
    final TermAllocator a = new TermAllocator();
    final ParceQ result =
        new ParceQ(a,
            Expression.of(List.of(
                a.createRootTerm("a"),
                a.createRootTerm("b"),
                a.createRootTerm("c"),
                a.createRootTerm("d"))))
        .termShingles(3, " ");
    assertThat(result.getExpression(),
        equalTo(
            or(
                termsExpr(term(0, "a"), term(1, "b"), term(2, "c"), term(3, "d")),
                termsExpr(term(4, "a b"), term(5, "b c"), term(6, "c d")),
                termsExpr(term(7, "a b c"), term(8, "b c d")))));

  }
}
