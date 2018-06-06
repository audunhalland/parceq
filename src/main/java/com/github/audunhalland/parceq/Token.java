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
  private final int offset;
  private final int length;

  Token(Type type, String value, int offset, int length) {
    this.type = type;
    this.value = value;
    this.offset = offset;
    this.length = length;
  }

  public Type getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return "{" + type + " " + value + "}";
  }

}
