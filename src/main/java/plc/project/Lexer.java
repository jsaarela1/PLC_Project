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
        while (chars.has(0)) {
            // check for \b\n\r\t
            if ((peek("[\\\\]")) && (chars.has(1))) {
                // get the index of the next character and check for if it needs to be skipped over
                char checkNext = chars.get(1);
                if ((checkNext == 'b') || (checkNext == 'n') || (checkNext == 'r') || (checkNext == 't')) {
                    chars.advance();
                    chars.skip();
                }
            }
            // check to make sure it is not a space
            else if (!peek("[ ]"))
                list.add(lexToken());
            // if it is a space
            else {
                chars.advance();
                chars.skip();
            }
        }

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
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } //else if (peek("[^ [\b]|[\n]|[\r]|[\t]]")) {
            //return lexOperator();
       // }
        //throw new UnsupportedOperationException();
        return lexOperator();
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

        boolean isNegative = false;
        boolean leadingZero = false;

        if (match("[0]")) {
            current++;
            leadingZero = true;
        }
        if (match("[-]")) {
            current++;
            isNegative = true;
            if (match("[0]"))
                leadingZero = true;
        }
        while (peek("[0-9]|[.]|[-]")) {
            if (isNegative && peek("[-]"))
                throw new ParseException("Invalid negative number at index: ", current);
            else if (makeDecimal && peek("[.]"))
                throw new ParseException("Invalid decimal at index: ", current);
            else if (leadingZero && (!peek("[.]")))
                throw new ParseException("Invalid number because of leading zero: ", current);
            else if (match("[-]")) {
                current++;
                isNegative = true;
            } else if (match("[.]")) {
                current++;
                makeDecimal = true;
                if (!peek("[0-9]"))
                    throw new ParseException("No value after the decimal point at index: ", current);
                // if it is a leading zero followed by a decimal, no error
                if (leadingZero) leadingZero = false;
            } else if (match("[0-9]")) {
                current++;
            }
        }
        // if 0 is first digit
        /*if (match("[0]")) {
            current++;
            if (match("[.]")) {
                makeDecimal = true;
                current++;
                while (match("[1-9]"))
                   current++;
            }
            // cannot have a leading 0
            else if (peek("[0-9]"))
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
         */
        if (isNegative && !makeDecimal && leadingZero)
            throw new ParseException("Cannot have a negative zero. Check index: ", current);
        if (makeDecimal)
            return chars.emit(Token.Type.DECIMAL);
        else
            return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        int current = chars.index;
        match("[']");
        current++;

        // check for the invalid characters
        if (peek("[\\\\]")) {
            match("[\\\\]");
            current++;
            System.out.print("CHECK");
            if (match("[^[n] | [r] | [\\\\]]"))
                current++;
            else
                throw new ParseException("Invalid character at index: ", current);
        } else if (match("[^']"))
            current++;
        else
            throw new ParseException("Invalid character at index: ", current);

        // check for a closing '
        if (match("[']"))
            current++;
        else
            throw new ParseException("Character not enclosed in single quote at index: ", current);
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        int current = chars.index;
        match("\"");
        current++;

        // run until a double quote
        while (peek("[^\"]")) {
            if (peek("[\\\\]")) {
                match("[\\\\]");
                current++;
                System.out.print("CHECK");
                if (match("[^[n] | [r] | [\\\\]]"))
                    current++;
                else
                    throw new ParseException("Invalid string at index: ", current);
            } else if (match("[^\"]"))
                current++;
        }
        // check for a closing '
        if (match("\""))
            current++;
        else
            throw new ParseException("Missing double quote at index: ", current);
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        System.out.println("escape");
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        int current = chars.index;
        if (match("[!]")) {
            current++;
            match("[=]");
        } else if (match("[=]")) {
            current++;
            if (peek("[=]")) {
                match("[=]");
                current++;
            }
        } else if (match("[&]")) {
            current++;
            match("[&]");
        } else if (match("[|]")) {
            current++;
            match("[|]");
        } else match("[^ ]");
        return chars.emit(Token.Type.OPERATOR);
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
