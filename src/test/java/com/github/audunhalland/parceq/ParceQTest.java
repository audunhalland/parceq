package com.github.audunhalland.parceq;

import static org.junit.Assert.*;
import static org.hamcrest.core.IsEqual.equalTo;

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
}
