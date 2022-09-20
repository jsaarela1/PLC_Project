package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List list = new ArrayList<>();
        list.add(lexToken());
        return list;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z]|[@]")) {
            return lexIdentifier();
        } else if (peek("[0]|[-]|[1-9]")) {
            return lexNumber(); // figure out how to differentiate integer vs a decimal
        } else if (peek("[']")) {
            lexCharacter();
        } else if (peek("\"")) {
            lexString();
        } else if (peek("\\")) {
            lexEscape();
        } else if (peek(" [!=]|[=]|[&]|[|]")) {
            lexOperator();
        } else if (peek(" [\b]|[\n]|[\r]|[\t]")) {
            // whitespace, thus
            chars.advance();
        } else {
            throw new UnsupportedOperationException(); //TODO
        }
        // also need to check for whitespace
        throw new UnsupportedOperationException();
    }

    public Token lexIdentifier() {
        int start = chars.index;
        int current = start;
        match("[A-Za-z]|[@]");
        current++;
        while (match("[A-Za-z0-9_-]")) {
            current++;
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        int current = chars.index; // keep track of current index, in case of errors
        boolean makeDecimal = false; // if a '.' is found, set to true and return a decimal
        // if 0 is first digit
        if (match("[0]")) {
            current++;
            if (match("[.]")) {
                makeDecimal = true;
                current++;
                while (match("[1-9]"))
                   current++;
            }
            // cannot have a leading 0
            else if (match("[0-9]"))
                throw new ParseException("Invalid leading zero at index: ", current);
        }
        // if negative
        else if (match("[-]")) {
            current++;
            if (match("[1-9]")) {
                current++;
                while (peek("[0-9]|[.]"))
                    if (match("[.]"))
                        makeDecimal = true;
                    else
                        match("[0-9]");
                    current++;
            } else
                throw new ParseException("Invalid negative number at index: ", current);
        }
        // everything else
        else {
            while (peek("[0-9]|[.]")) {
                if (match("[.]")) {
                    makeDecimal = true;
                    if (!peek("[0-9]"))
                        throw new ParseException("No numbers after the decimal point at index: ", current);
                }
                else
                    match("[0-9]");
                current++;
            }
        }
        if (makeDecimal)
            return chars.emit(Token.Type.DECIMAL);
        else
            return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        int start = chars.index;
        int current = start;
        match("[']");
        current++;
        if (match("[\\]")) {
            current++;
            if (match("[']")) {
                current++;
            } else
                throw new ParseException("Invalid character at index ", current);
        } else
            throw new ParseException("Invalid character at index ", current);
        System.out.println("Character of length: " + (current-start));
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        System.out.println("string");
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        System.out.println("escape");
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        System.out.println("operator");
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]))
                return false;
        }
        return true;
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
