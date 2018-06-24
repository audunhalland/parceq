package com.github.audunhalland.parceq;

import io.vavr.control.Option;
import java.util.Objects;

public class Term {
  private final int id;
  private final String value;
  private final Option<String> field;

  public Term(int id, String value) {
    this.id = id;
    this.value = value;
    this.field = Option.none();
  }

  public int getId() {
    return id;
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
        && id == ((Term) other).id
        && value.equals(((Term) other).value)
        && field.equals(((Term) other).field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, value, field);
  }
}
