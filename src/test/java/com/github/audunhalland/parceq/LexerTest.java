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

  private static Token word(String word) {
    return new Token(Token.Type.WORD, word);
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
    assertTokens("\n", word("\n"), EOF);
    assertTokens(" \n ", word("\n"), EOF);
    assertTokens("\n a", word("\n"), word("a"), EOF);
    assertTokens("\n abc", word("\n"), word("abc"), EOF);
    assertTokens("\n abc ", word("\n"), word("abc"), EOF);
  }

  @Test
  public void escaping_regular_characters_has_no_effect() {
    assertTokens("\\abc", word("\\abc"), EOF);
    assertTokens("\\\\abc", word("\\\\abc"), EOF);
    assertTokens("a\\bc", word("a\\bc"), EOF);
    assertTokens("ab\\\\c", word("ab\\\\c"), EOF);
    assertTokens("ab\\\\\\c", word("ab\\\\\\c"), EOF);
    assertTokens("abc\\", word("abc\\"), EOF);
    assertTokens("abc\\\\", word("abc\\\\"), EOF);
  }

  @Test
  public void tokenizes_parentheses() {
    assertTokens("(", LEFT_PAREN, EOF);
    assertTokens(")", RIGHT_PAREN, EOF);
    assertTokens("a(", word("a"), LEFT_PAREN, EOF);
    assertTokens("a))", word("a"), RIGHT_PAREN, RIGHT_PAREN, EOF);
    assertTokens("a)(b", word("a"), RIGHT_PAREN, LEFT_PAREN, word("b"), EOF);
    assertTokens("a ( ) b", word("a"), LEFT_PAREN, RIGHT_PAREN, word("b"), EOF);
    assertTokens("a ( b) c", word("a"), LEFT_PAREN, word("b"), RIGHT_PAREN, word("c"), EOF);
  }

  @Test
  public void tokenizes_prefix_operators() {
    assertTokens("+", PREFIX_AND, EOF);
    assertTokens("-", PREFIX_AND_NOT, EOF);
    assertTokens("+-", PREFIX_AND, PREFIX_AND_NOT, EOF);
    assertTokens("+abc", PREFIX_AND, word("abc"), EOF);
    assertTokens("+foo-bar", PREFIX_AND, word("foo-bar"), EOF);
    assertTokens("+foo -bar", PREFIX_AND, word("foo"), PREFIX_AND_NOT, word("bar"), EOF);
    assertTokens("+foo -+bar", PREFIX_AND, word("foo"), PREFIX_AND_NOT, PREFIX_AND, word("bar"), EOF);
  }

  @Test
  public void tokenizes_infix_operators() {
    assertTokens("AND", token(Token.Type.INFIX_AND, "AND"), EOF);
    assertTokens("&&", token(Token.Type.INFIX_AND, "&&"), EOF);
    assertTokens("OR", token(Token.Type.INFIX_OR, "OR"), EOF);
    assertTokens("||", token(Token.Type.INFIX_OR, "||"), EOF);
    assertTokens("&&&", word("&&&"), EOF);
  }

  @Test
  public void tokenizes_quoted_words() {
    assertTokens("\"foo\"", word("foo"), EOF);
    assertTokens("\"foo bar\"", word("foo bar"), EOF);
    assertTokens("\"foo bar\"\"baz\"", word("foo bar"), word("baz"), EOF);
    assertTokens("\"foo bar \" \"baz\"", word("foo bar "), word("baz"), EOF);
    assertTokens("-\"foo bar\"+baz", PREFIX_AND_NOT, word("foo bar"), PREFIX_AND, word("baz"), EOF);
  }

  @Test
  public void quoting_rules_are_lenient() {
    assertTokens("\"foo", word("foo"), EOF);
    assertTokens("foo \"", word("foo"), EOF);
  }

  @Test
  public void in_word_quotes_does_not_trigger_new_word() {
    assertTokens("foo\"bar", word("foo\"bar"), EOF);
  }

  @Test
  public void escapes_quotes() {
    assertTokens("\\\"foo bar", word("\"foo"), word("bar"), EOF);
    assertTokens("\"foo\\\" bar", word("foo\" bar"), EOF);
    assertTokens("foo \"bar\\baz", word("foo"), word("bar\\baz"), EOF);
    assertTokens("foo \"bar\\\\baz", word("foo"), word("bar\\\\baz"), EOF);
  }

  @Test
  public void escapes_operators() {
    assertTokens("\\-foo", word("-foo"), EOF);
    assertTokens("\\-+foo", word("-+foo"), EOF);
    assertTokens("foo \\(bar", word("foo"), word("(bar"), EOF);
  }

  @Test
  public void handles_utf8() {
    assertTokens("føø bær", word("føø"), word("bær"), EOF);
  }

}
