package com.github.audunhalland.parceq;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.io.Reader;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    return new ParceQ(termAllocator,
        expr.flatMapTerms(terms ->
            Expression.of(
                Util.<Term>shingler(order)
                    .apply(terms)
                    .map(shingledTerms ->
                        shingledTerms.length() == 1
                            ? shingledTerms.get(0)
                            : termAllocator.createDerivedTerm(
                                shingledTerms.map(Term::getValue)
                                    .collect(Collectors.joining(separator)),
                              shingledTerms))
                    .toList())));

  }
}
