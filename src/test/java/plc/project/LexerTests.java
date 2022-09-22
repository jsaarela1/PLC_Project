package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Contains underscore", "go_gators", true),
                Arguments.of("Contains hyphen", "lets-go", true),
                Arguments.of("Starts with an @", "@captainHook", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading Underscore", "_goodTry", false),
                Arguments.of("Contains @ in middle", "this@stinks", false),
                Arguments.of("Has @ throughout", "@1@2@3@4time", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Trailing Zeros", "1230000", true),
                Arguments.of("Zero", "0", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Negative with 0", "-0", false),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Decimal less than 1", "0.42", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Zeros", "5.200000", true),
                Arguments.of("No decimal point", "45", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading zero", "04.12", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Slash", "\'\\\\\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }
// made tests up to here
// still needs to make more unit tests for testString and testOperator
    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("AND", "&&", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "for (int i = 0; i < 10) {", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "for", 0),
                        new Token(Token.Type.OPERATOR, "(", 4),
                        new Token(Token.Type.IDENTIFIER, "int",5),
                        new Token(Token.Type.IDENTIFIER, "i",9),
                        new Token(Token.Type.OPERATOR, "=", 11),
                        new Token(Token.Type.INTEGER, "0",13),
                        new Token(Token.Type.OPERATOR, ";",14),
                        new Token(Token.Type.IDENTIFIER, "i",16),
                        new Token(Token.Type.OPERATOR, "<",18),
                        new Token(Token.Type.INTEGER, "10", 20),
                        new Token(Token.Type.OPERATOR, ")",22),
                        new Token(Token.Type.OPERATOR, "{", 24)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }
/*
    @ParameterizedTest
    @MethodSource
    void testPeekSingleChar(String test, String source, boolean success, String pattern) {
        Assertions.assertEquals(success, new Lexer(source).peek(pattern));
    }

    private static Stream<Arguments> testPeekSingleChar() {
        return Stream.of(
                Arguments.of("first char 0", "012", true, "0"),
                Arguments.of("first char 1", "012", false, "1"),
                Arguments.of("first char not digit", "3", true, "\\d")
        );
    }

    @Test
    void testPeekMultiCharTrue() {
        Assertions.assertTrue(new Lexer("4123420").peek("4", "1", "2"));
    }

    @Test
    void testPeekMultiCharFalse() {
        Assertions.assertFalse(new Lexer("12345").peek("1", "2", "2", "3", "4", "7"));
    }

    @ParameterizedTest
    @MethodSource
    void testMatch(String test, String source, boolean success, String... patterns) {
        Assertions.assertEquals(success, new Lexer(source).match(patterns));
    }

    private static Stream<Arguments> testMatch() {
        String strings[] = {"c","="};
        return Stream.of(
                Arguments.of("Testing 2", "dc=", true, strings)
        );
    }

*/

}
