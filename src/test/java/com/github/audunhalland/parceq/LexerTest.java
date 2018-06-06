package com.github.audunhalland.parceq;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import java.io.StringReader;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class LexerTest {
  private final Token LEFT_PAREN = new Token(Token.Type.LEFT_PAREN, "(");
  private final Token RIGHT_PAREN = new Token(Token.Type.RIGHT_PAREN, ")");
  private final Token PREFIX_AND = new Token(Token.Type.PREFIX_AND, "+");
  private final Token PREFIX_AND_NOT = new Token(Token.Type.PREFIX_ANDNOT, "-");
  private final Token EOF = new Token(Token.Type.EOF, "");

  private Lexer lexer;

  @Before
  public void setUp() {
    lexer = new Lexer();
  }

  private static Tuple2<Type, String> tokenToTuple(Token token) {
    return new Tuple2<>(token.getType(), token.getValue());
  }

  private void assertEof(String query) {
    assertThat(
        lexer.tokenStream(new StringReader(query))
            .map(Try::get)
            .map(Token::getType)
            .asJava(),
        equalTo(Collections.singletonList(Token.Type.EOF)));
  }

  private void assertTokens(String query, Token ... tokens) {
    assertThat(
        lexer.tokenStream(new StringReader(query))
            .map(Try::get)
            .map(LexerTest::tokenToTuple)
            .asJava(),
        equalTo(List.of(tokens)
            .map(LexerTest::tokenToTuple)
            .asJava()));
  }

  private static Token phrase(String phrase) {
    return new Token(Token.Type.PHRASE, phrase);
  }

  private static Token token(Type type, String value) {
    return new Token(type, value);
  }

  @Test
  public void empty_and_whitespace_only_has_no_tokens() {
    assertEof("");
    assertEof(" ");
    assertEof("  ");
  }

  @Test
  public void tokenizes_simple_terms() {
    assertTokens("\n", phrase("\n"), EOF);
    assertTokens(" \n ", phrase("\n"), EOF);
    assertTokens("\n a", phrase("\n"), phrase("a"), EOF);
    assertTokens("\n abc", phrase("\n"), phrase("abc"), EOF);
    assertTokens("\n abc ", phrase("\n"), phrase("abc"), EOF);
  }

  @Test
  public void escaping_regular_characters_has_no_effect() {
    assertTokens("\\abc", phrase("\\abc"), EOF);
    assertTokens("\\\\abc", phrase("\\\\abc"), EOF);
    assertTokens("a\\bc", phrase("a\\bc"), EOF);
    assertTokens("ab\\\\c", phrase("ab\\\\c"), EOF);
    assertTokens("ab\\\\\\c", phrase("ab\\\\\\c"), EOF);
    assertTokens("abc\\", phrase("abc\\"), EOF);
    assertTokens("abc\\\\", phrase("abc\\\\"), EOF);
  }

  @Test
  public void tokenizes_parentheses() {
    assertTokens("(", LEFT_PAREN, EOF);
    assertTokens(")", RIGHT_PAREN, EOF);
    assertTokens("a(", phrase("a"), LEFT_PAREN, EOF);
    assertTokens("a))", phrase("a"), RIGHT_PAREN, RIGHT_PAREN, EOF);
    assertTokens("a)(b", phrase("a"), RIGHT_PAREN, LEFT_PAREN, phrase("b"), EOF);
    assertTokens("a ( ) b", phrase("a"), LEFT_PAREN, RIGHT_PAREN, phrase("b"), EOF);
    assertTokens("a ( b) c", phrase("a"), LEFT_PAREN, phrase("b"), RIGHT_PAREN, phrase("c"), EOF);
  }

  @Test
  public void tokenizes_prefix_operators() {
    assertTokens("+", PREFIX_AND, EOF);
    assertTokens("-", PREFIX_AND_NOT, EOF);
    assertTokens("+-", PREFIX_AND, PREFIX_AND_NOT, EOF);
    assertTokens("+abc", PREFIX_AND, phrase("abc"), EOF);
    assertTokens("+foo-bar", PREFIX_AND, phrase("foo-bar"), EOF);
    assertTokens("+foo -bar", PREFIX_AND, phrase("foo"), PREFIX_AND_NOT, phrase("bar"), EOF);
    assertTokens("+foo -+bar", PREFIX_AND, phrase("foo"), PREFIX_AND_NOT, PREFIX_AND, phrase("bar"), EOF);
  }

  @Test
  public void tokenizes_infix_operators() {
    assertTokens("AND", token(Token.Type.INFIX_AND, "AND"), EOF);
    assertTokens("&&", token(Token.Type.INFIX_AND, "&&"), EOF);
    assertTokens("OR", token(Token.Type.INFIX_OR, "OR"), EOF);
    assertTokens("||", token(Token.Type.INFIX_OR, "||"), EOF);
    assertTokens("&&&", phrase("&&&"), EOF);
  }

  @Test
  public void tokenizes_quoted_phrases() {
    assertTokens("\"foo\"", phrase("foo"), EOF);
    assertTokens("\"foo bar\"", phrase("foo bar"), EOF);
    assertTokens("\"foo bar\"\"baz\"", phrase("foo bar"), phrase("baz"), EOF);
    assertTokens("\"foo bar \" \"baz\"", phrase("foo bar "), phrase("baz"), EOF);
    assertTokens("-\"foo bar\"+baz", PREFIX_AND_NOT, phrase("foo bar"), PREFIX_AND, phrase("baz"), EOF);
  }

  @Test
  public void quoting_rules_are_lenient() {
    assertTokens("\"foo", phrase("foo"), EOF);
    assertTokens("foo \"", phrase("foo"), EOF);
  }

  @Test
  public void in_phrase_quotes_does_not_trigger_new_phrase() {
    assertTokens("foo\"bar", phrase("foo\"bar"), EOF);
  }

  @Test
  public void escapes_quotes() {
    assertTokens("\\\"foo bar", phrase("\"foo"), phrase("bar"), EOF);
    assertTokens("\"foo\\\" bar", phrase("foo\" bar"), EOF);
    assertTokens("foo \"bar\\baz", phrase("foo"), phrase("bar\\baz"), EOF);
    assertTokens("foo \"bar\\\\baz", phrase("foo"), phrase("bar\\\\baz"), EOF);
  }

  @Test
  public void escapes_operators() {
    assertTokens("\\-foo", phrase("-foo"), EOF);
    assertTokens("\\-+foo", phrase("-+foo"), EOF);
    assertTokens("foo \\(bar", phrase("foo"), phrase("(bar"), EOF);
  }

  @Test
  public void handles_utf8() {
    assertTokens("føø bær", phrase("føø"), phrase("bær"), EOF);
  }

}
