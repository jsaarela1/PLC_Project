package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Includes a Dot before the @", "today.willB3gr8@yahoo.com", true),
                Arguments.of("Number after the @ before the Dot", "scoobydoo12@c0mcast.net", true),
                Arguments.of("Includes an Underscore", "jack_342@yahoo.com", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Symbol after the dot", "letsgo@yahoo.c/m", false),
                Arguments.of("Number after the dot", "letstryagain@yahoo.cm2", false),
                Arguments.of("More than 3 characters after the Dot", "johnnyapples33d@aol.government", false),
                Arguments.of("Symbol after the @ before the Dot", "whatAshame3@gma!l.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("15 Characters", "iNseptember2022", true),
                Arguments.of("17 Characters", "testingoddstrings", true),
                Arguments.of("19 Characters", "programmingisfun153", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("10 Characters", "saturday81", false),
                Arguments.of("16 Characters", "gr8tob3aFlGator!", false),
                Arguments.of("22 Characters", "thisWillb3twenty2Chars",false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Empty", "[]", true),
                Arguments.of("Mixed Spaces", "['a', 'b','c','d', 'e']", true),
                Arguments.of("Space After Comma", "['a', 'b', 'c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Trailing Comma", "['a','b','c',]", false),
                Arguments.of("Missing Single Quote", "['a','b]", false),
                Arguments.of("Space Before Comma", "['a' ,'b']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
        //throw new UnsupportedOperationException(); //TODO
    }

    public static Stream<Arguments> testDecimalRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Large Value", "10100.001", true),
                Arguments.of("Negative Value", "-1.0", true),
                Arguments.of("Trailing Zeros", "1.320000000", true),
                Arguments.of("Number Less Than One", "0.342", true),
                Arguments.of("Many Zero Before Decimal","90000000000.2", true),
                Arguments.of("No Decimal", "1", false),
                Arguments.of("No Digit Before Decimal", ".5", false),
                Arguments.of("Zero Leftest Digit", "0103.2003", false),
                Arguments.of("Multiple Decimals", "1.342.12", false),
                Arguments.of("Negative Sign After Number", "2.42-", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
        //throw new UnsupportedOperationException(); //TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Special Characters Included", "\"Hello, World!\"", true),
                Arguments.of("Escape Characters", "\"1\\t2\"", true),
                Arguments.of("Multiple Escape Characters", "\"here\\b we\\' go\\\\\"", true),
                Arguments.of("Only escape characters", "\"\b\n\'\"", true),
                Arguments.of("Unclosed String", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Characters Before String", "oops\"you can't do this\"", false),
                Arguments.of("Characters After String", "\"this was so close!\" almost", false),
                Arguments.of("Single Quoted String", "\"nice try'", false),
                Arguments.of("Just A Backslash", "\"\\\"", false)
        );
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
