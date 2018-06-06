package com.github.audunhalland.parceq;

public class Token {
  public enum Type {
    PHRASE(80),
    LEFT_PAREN(0),
    RIGHT_PAREN(0),
    INFIX_AND(10),
    INFIX_OR(20),
    PREFIX_AND(50),
    PREFIX_ANDNOT(50),
    EOF(0);

    public final int leftBindingPower;

    Type(int leftBindingPower) {
      this.leftBindingPower = leftBindingPower;
    }
  }

  private final Type type;
  private final String value;

  Token(Type type, String value) {
    this.type = type;
    this.value = value;
  }

  public Type getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "{" + type + " " + value + "}";
  }

}
