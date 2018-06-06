package com.github.audunhalland.parceq;

import io.vavr.collection.List;
import io.vavr.control.Option;

public class Lexer {

  public List<Token> tokenize(String query) {
    final int length = query.length();
    List<Token> tokens = List.empty();

    int offset = 0;

    while (true) {
      final Option<Token> optNext = getNextToken(query, offset, length);
      if (optNext.isEmpty()) {
        break;
      }

      final Token next = optNext.get();
      tokens = tokens.append(next);
      offset += next.getLength();
    }

    return tokens;
  }

  private Option<Token> getNextToken(String query, int startOffset, int length) {
    if (startOffset >= length) {
      return Option.none();
    }

    final StringBuilder builder = new StringBuilder();

    boolean escaped = false;
    int offset = startOffset;

    while (offset < length) {
      final int codepoint = query.codePointAt(offset);
      offset += Character.charCount(codepoint);

      switch (codepoint) {
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
            builder.appendCodePoint('\\');
            return readUnquotedPhrase(query, offset, length, startOffset, builder);
          }
          escaped = true;
          break;
        case '(':
        case ')':
        case '-':
        case '+':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            return readUnquotedPhrase(query, offset, length, startOffset, builder);
          } else {
            return createSingleCharToken(codepoint, startOffset, offset - startOffset);
          }
        case '"':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            return readUnquotedPhrase(query, offset, length, startOffset, builder);
          } else {
            return readQuotedPhrase(query, offset, length, startOffset);
          }
        case ' ':
          break;
        default:
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          builder.appendCodePoint(codepoint);
          return readUnquotedPhrase(query, offset, length, startOffset, builder);
      }
    }

    // white space only - not considered a token
    return Option.none();
  }

  private Option<Token> createSingleCharToken(int codepoint, int startOffset, int length) {
    switch (codepoint) {
      case '(':
        return Option.of(new Token(Token.Type.LEFT_PAREN, "(", startOffset, length));
      case ')':
        return Option.of(new Token(Token.Type.RIGHT_PAREN, ")", startOffset, length));
      case '-':
        return Option.of(new Token(Token.Type.PREFIX_ANDNOT, "-", startOffset, length));
      case '+':
        return Option.of(new Token(Token.Type.PREFIX_AND, "+", startOffset, length));
      default:
        return Option.none();
    }
  }

  private Option<Token> readUnquotedPhrase(String query, int offset, int length, int tokenStart,
      StringBuilder builder) {
    boolean escaped = false;

    while (offset < length) {
      final int codepoint = query.codePointAt(offset);
      offset += Character.charCount(codepoint);

      switch (codepoint) {
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          escaped = true;
          break;
        case '(':
        case ')':
        case ' ':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            escaped = false;
          } else {
            return Option.of(
                createUnquotedToken(builder.toString(), tokenStart, offset - tokenStart - 1));
          }
          break;
        default:
          if (escaped) {
            builder.appendCodePoint('\\');
            escaped = false;
          }
          builder.appendCodePoint(codepoint);
          break;
      }
    }

    if (escaped) {
      builder.appendCodePoint('\\');
    }

    return Option.of(
        createUnquotedToken(builder.toString(), tokenStart, offset - tokenStart));
  }

  private Token createUnquotedToken(String token, int offset, int length) {
    switch (token) {
      case "AND":
      case "&&":
        return new Token(Token.Type.INFIX_AND, token, offset, length);
      case "OR":
      case "||":
        return new Token(Token.Type.INFIX_OR, token, offset, length);
      default:
        return new Token(Token.Type.PHRASE, token, offset, length);
    }
  }

  private Option<Token> readQuotedPhrase(String query, int offset, int length, int tokenStart) {
    final StringBuilder builder = new StringBuilder();
    boolean escaped = false;
    while (offset < length) {
      final int codepoint = query.codePointAt(offset);
      offset += Character.charCount(codepoint);

      switch (codepoint) {
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          escaped = true;
          break;
        case '"':
          if (!escaped) {
            return Option.of(
                new Token(Token.Type.PHRASE, builder.toString(), tokenStart, offset - tokenStart));
          }
          escaped = false;
          // fall through
        default:
          // escape only works on quote
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          escaped = false;
          builder.appendCodePoint(codepoint);
          break;
      }
    }

    // Tolerate missing closing parenthesis at end of string
    if (builder.length() > 0) {
      return Option.of(
          new Token(Token.Type.PHRASE, builder.toString(), tokenStart, offset - tokenStart));
    } else {
      // EOF following an opening quote - does not count as phrase
      return Option.none();
    }
  }
}
