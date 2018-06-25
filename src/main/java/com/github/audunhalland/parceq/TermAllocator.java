package com.github.audunhalland.parceq;

import io.vavr.collection.List;

public class TermAllocator {
  private int counter;

  public TermAllocator() {
    counter = 0;
  }

  public Term createRootTerm(String value) {
    return new Term(counter++, value);
  }

  public Term createDerivedTerm(String value, List<Term> derivees) {
    return new Term(counter++, value);
  }
}
