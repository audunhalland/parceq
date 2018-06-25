package com.github.audunhalland.parceq;

import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.io.Reader;

public class ParceQ {
  private final TermAllocator termAllocator;
  private final Expression expr;

  ParceQ(TermAllocator termAllocator, Expression expr) {
    this.termAllocator = termAllocator;
    this.expr = expr;
  }

  public static Try<ParceQ> parse(Reader reader) {
    final TermAllocator termAllocator = new TermAllocator();
    final Stream<Try<Token>> tokens = new Lexer().tokenStream(reader);
    return Try.of(() -> {
      final Stream<Token> tokensSuccess = tokens.map(Try::get);
      return new ParceQ(termAllocator,
          new Parser(termAllocator).parse(tokensSuccess));
    });
  }

  public Expression getExpression() {
    return expr;
  }

  public ParceQ termShingles(int order, CharSequence separator) {
    return null;
  }
}
