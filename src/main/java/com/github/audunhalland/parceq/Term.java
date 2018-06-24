package com.github.audunhalland.parceq;

import com.github.audunhalland.parceq.Expression.SubExpression;
import io.vavr.control.Option;
import java.util.Objects;

public class Term {
  private final String value;
  private final Option<String> field;

  public Term(String value) {
    this.value = value;
    this.field = Option.none();
  }

  public String getValue() {
    return value;
  }

  public Option<String> getField() {
    return field;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    return other instanceof Term
        && value == ((Term) other).value
        && field.equals(((Term) other).field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, field);
  }
}
