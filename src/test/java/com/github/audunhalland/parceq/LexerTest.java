package com.github.audunhalland.parceq;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import com.github.audunhalland.parceq.Token.Type;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class LexerTest {
  private final Token LEFT_PAREN = new Token(Token.Type.LEFT_PAREN, "(", 0, 0);
  private final Token RIGHT_PAREN = new Token(Token.Type.RIGHT_PAREN, ")", 0, 0);
  private final Token PREFIX_AND = new Token(Token.Type.PREFIX_AND, "+", 0, 0);
  private final Token PREFIX_AND_NOT = new Token(Token.Type.PREFIX_ANDNOT, "-", 0, 0);

  private Lexer lexer;

  @Before
  public void setUp() {
    lexer = new Lexer();
  }

  private static Tuple2<Type, String> tokenToTuple(Token token) {
    return new Tuple2<>(token.getType(), token.getValue());
  }

  private void assertNoTokens(String query) {
    assertThat(
        lexer.tokenize(query).map(Token::getType).asJava(),
        equalTo(Collections.emptyList()));
  }

  private void assertTokens(String query, Token ... tokens) {
    assertThat(
        lexer.tokenize(query)
            .map(LexerTest::tokenToTuple)
            .asJava(),
        equalTo(List.of(tokens)
            .map(LexerTest::tokenToTuple)
            .asJava()));
  }

  private static Token phrase(String phrase) {
    return new Token(Token.Type.PHRASE, phrase, 0, 0);
  }

  private static Token token(Type type, String value) {
    return new Token(type, value, 0, 0);
  }

  @Test
  public void empty_and_whitespace_only_has_no_tokens() {
    assertNoTokens("");
    assertNoTokens(" ");
    assertNoTokens("  ");
  }

  @Test
  public void tokenizes_simple_terms() {
    assertTokens("\n", phrase("\n"));
    assertTokens(" \n ", phrase("\n"));
    assertTokens("\n a", phrase("\n"), phrase("a"));
    assertTokens("\n abc", phrase("\n"), phrase("abc"));
    assertTokens("\n abc ", phrase("\n"), phrase("abc"));
  }

  @Test
  public void escaping_regular_characters_has_no_effect() {
    assertTokens("\\abc", phrase("\\abc"));
    assertTokens("\\\\abc", phrase("\\\\abc"));
    assertTokens("a\\bc", phrase("a\\bc"));
    assertTokens("ab\\\\c", phrase("ab\\\\c"));
    assertTokens("ab\\\\\\c", phrase("ab\\\\\\c"));
    assertTokens("abc\\", phrase("abc\\"));
    assertTokens("abc\\\\", phrase("abc\\\\"));
  }

  @Test
  public void tokenizes_parentheses() {
    assertTokens("(", LEFT_PAREN);
    assertTokens(")", RIGHT_PAREN);
    assertTokens("a(", phrase("a"), LEFT_PAREN);
    assertTokens("a))", phrase("a"), RIGHT_PAREN, RIGHT_PAREN);
    assertTokens("a)(b", phrase("a"), RIGHT_PAREN, LEFT_PAREN, phrase("b"));
    assertTokens("a ( ) b", phrase("a"), LEFT_PAREN, RIGHT_PAREN, phrase("b"));
    assertTokens("a ( b) c", phrase("a"), LEFT_PAREN, phrase("b"), RIGHT_PAREN, phrase("c"));
  }

  @Test
  public void tokenizes_prefix_operators() {
    assertTokens("+", PREFIX_AND);
    assertTokens("-", PREFIX_AND_NOT);
    assertTokens("+-", PREFIX_AND, PREFIX_AND_NOT);
    assertTokens("+abc", PREFIX_AND, phrase("abc"));
    assertTokens("+foo-bar", PREFIX_AND, phrase("foo-bar"));
    assertTokens("+foo -bar", PREFIX_AND, phrase("foo"), PREFIX_AND_NOT, phrase("bar"));
    assertTokens("+foo -+bar", PREFIX_AND, phrase("foo"), PREFIX_AND_NOT, PREFIX_AND, phrase("bar"));
  }

  @Test
  public void tokenizes_infix_operators() {
    assertTokens("AND", token(Token.Type.INFIX_AND, "AND"));
    assertTokens("&&", token(Token.Type.INFIX_AND, "&&"));
    assertTokens("OR", token(Token.Type.INFIX_OR, "OR"));
    assertTokens("||", token(Token.Type.INFIX_OR, "||"));
    assertTokens("&&&", phrase("&&&"));
  }

  @Test
  public void tokenizes_quoted_phrases() {
    assertTokens("\"foo\"", phrase("foo"));
    assertTokens("\"foo bar\"", phrase("foo bar"));
    assertTokens("\"foo bar\"\"baz\"", phrase("foo bar"), phrase("baz"));
    assertTokens("\"foo bar \" \"baz\"", phrase("foo bar "), phrase("baz"));
    assertTokens("-\"foo bar\"+baz", PREFIX_AND_NOT, phrase("foo bar"), PREFIX_AND, phrase("baz"));
  }

  @Test
  public void quoting_rules_are_lenient() {
    assertTokens("\"foo", phrase("foo"));
    assertTokens("foo \"", phrase("foo"));
  }

  @Test
  public void in_phrase_quotes_does_not_trigger_new_phrase() {
    assertTokens("foo\"bar", phrase("foo\"bar"));
  }

  @Test
  public void escapes_quotes() {
    assertTokens("\\\"foo bar", phrase("\"foo"), phrase("bar"));
    assertTokens("\"foo\\\" bar", phrase("foo\" bar"));
    assertTokens("foo \"bar\\baz", phrase("foo"), phrase("bar\\baz"));
    assertTokens("foo \"bar\\\\baz", phrase("foo"), phrase("bar\\\\baz"));
  }

  @Test
  public void escapes_operators() {
    assertTokens("\\-foo", phrase("-foo"));
    assertTokens("\\-+foo", phrase("-+foo"));
    assertTokens("foo \\(bar", phrase("foo"), phrase("(bar"));
  }

  @Test
  public void handles_utf8() {
    assertTokens("føø bær", phrase("føø"), phrase("bær"));
  }

}
