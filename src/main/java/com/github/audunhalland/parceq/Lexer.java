package com.github.audunhalland.parceq;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.Reader;

public class Lexer {

  public Stream<Try<Token>> tokenStream(Reader reader) {
    final StringBuilder builder = new StringBuilder();

    boolean escaped = false;

    while (true) {
      final int codepoint;
      try {
        codepoint = reader.read();
      } catch (IOException e) {
        return Stream.of(Try.failure(e));
      }

      switch (codepoint) {
        case -1:
          // white space only - not considered a token
          return eof();
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
            builder.appendCodePoint('\\');
            return unquoted(builder, reader);
          }
          escaped = true;
          break;
        case '(':
        case ')':
        case '-':
        case '+':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            return unquoted(builder, reader);
          } else {
            return singleChar(codepoint, reader);
          }
        case '"':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            return unquoted(builder, reader);
          } else {
            return quoted(reader);
          }
        case ' ':
          break;
        default:
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          builder.appendCodePoint(codepoint);
          return unquoted(builder, reader);
      }
    }
  }

  private Stream<Try<Token>> yield(Try<Token> token, Reader reader) {
    return Stream.cons(token, () -> tokenStream(reader));
  }

  private Stream<Try<Token>> yield(Token token, Reader reader) {
    return yield(Try.success(token), reader);
  }

  private Stream<Try<Token>> eof() {
    return Stream.of(Try.success(new Token(Type.EOF, "")));
  }

  private Stream<Try<Token>> eof(Try<Token> token) {
    return Stream.of(token, Try.success(new Token(Type.EOF, "")));
  }

  private Stream<Try<Token>> singleChar(int codepoint, Reader reader) {
    switch (codepoint) {
      case '(':
        return yield(new Token(Token.Type.LEFT_PAREN, "("), reader);
      case ')':
        return yield(new Token(Token.Type.RIGHT_PAREN, ")"), reader);
      case '-':
        return yield(new Token(Token.Type.PREFIX_ANDNOT, "-"), reader);
      case '+':
        return yield(new Token(Token.Type.PREFIX_AND, "+"), reader);
      default:
        return eof();
    }
  }

  private Stream<Try<Token>> unquoted(StringBuilder builder, Reader reader) {
    boolean escaped = false;

    while (true) {
      final int codepoint;
      try {
        codepoint = reader.read();
      } catch (IOException e) {
        return Stream.of(Try.failure(e));
      }

      switch (codepoint) {
        case -1:
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          return eof(unquoted(builder.toString()));
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          escaped = true;
          break;
        case '(':
        case ')':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            escaped = false;
          } else {
            return Stream.cons(unquoted(builder.toString()),
                () -> singleChar(codepoint, reader));
          }
        case ' ':
          if (escaped) {
            builder.appendCodePoint(codepoint);
            escaped = false;
          } else {
            return yield(unquoted(builder.toString()), reader);
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
  }

  private Try<Token> unquoted(String word) {
    switch (word) {
      case "AND":
      case "&&":
        return Try.success(new Token(Token.Type.INFIX_AND, word));
      case "OR":
      case "||":
        return Try.success(new Token(Token.Type.INFIX_OR, word));
      default:
        return Try.success(new Token(Token.Type.PHRASE, word));
    }
  }

  private Stream<Try<Token>> quoted(Reader reader) {
    final StringBuilder builder = new StringBuilder();
    boolean escaped = false;
    while (true) {
      final int codepoint;
      try {
        codepoint = reader.read();
      } catch (IOException e) {
        return Stream.of(Try.failure(e));
      }

      switch (codepoint) {
        case -1:
          // Tolerate missing closing parenthesis at end of string
          if (builder.length() > 0) {
            return eof(Try.success(new Token(Type.PHRASE, builder.toString())));
          } else {
            // EOF following an opening quote - does not count as phrase
            return eof();
          }
        case '\\':
          if (escaped) {
            builder.appendCodePoint('\\');
          }
          escaped = true;
          break;
        case '"':
          if (!escaped) {
            return yield(new Token(Type.PHRASE, builder.toString()), reader);
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
  }
}
