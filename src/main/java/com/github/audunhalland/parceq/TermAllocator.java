package com.github.audunhalland.parceq;

public class TermAllocator {
  private int counter;

  public TermAllocator() {
    counter = 0;
  }

  public Term createRootTerm(String value) {
    return new Term(counter++, value);
  }
}
