package com.github.audunhalland.parceq;

import io.vavr.collection.Iterator;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.io.Reader;
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
          Expression.of(Operator.OR,
              Stream.concat(
                  Stream.ofAll(terms)
                      .map(Expression::of),
                  Stream.range(2, order + 1)
                      .map(n -> terms
                          .sliding(n)
                          .map(group -> termAllocator.createRootTerm(
                              group.map(Term::getValue)
                                  .collect(Collectors.joining(separator)))))
                      .map(Iterator::toList)
                      .map(Expression::of))
                  .toList())));
  }
}
