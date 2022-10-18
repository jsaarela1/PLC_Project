package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Expression leftExpression = parseExpression();
        Ast.Statement statement = new Ast.Statement.Expression(leftExpression);
        while (peek(Token.Type.OPERATOR)) {
            String str = tokens.get(0).toString();
            str = str.substring(9,10);
            if (str.equals("=")) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseExpression();
                statement = new Ast.Statement.Assignment(leftExpression, rightExpression);
            }
            else {
                break;
            }
        }
        return statement;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException();//TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        Ast.Expression expression = parseLogicalExpression();
        return expression;
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression leftExpression = parseComparisonExpression();
        while (peek(Token.Type.OPERATOR)) {
            String str = tokens.get(0).toString();
            str = str.substring(9,11);
            if ((str.equals("&&")) || (str.equals("||"))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseLogicalExpression();
                leftExpression = new Ast.Expression.Binary(operator,leftExpression,rightExpression);
            }
            else {
                break;
            }
        }
        return leftExpression;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression leftExpression = parseAdditiveExpression();
        while (peek(Token.Type.OPERATOR)) {
            String str = tokens.get(0).toString();
            String str1 = str.substring(9,10);
            String str2 = str.substring(9,11);
            if ((str1.equals("<")) || (str1.equals(">"))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseLogicalExpression();
                leftExpression = new Ast.Expression.Binary(operator,leftExpression,rightExpression);
            }
            else if ((str2.equals("==")) || (str2.equals("!="))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseLogicalExpression();
                leftExpression = new Ast.Expression.Binary(operator,leftExpression,rightExpression);
            }
            else {
                break;
            }
        }
        return leftExpression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() {
        Ast.Expression leftExpression = parseMultiplicativeExpression();
        while (peek(Token.Type.OPERATOR)) {
            String str = tokens.get(0).toString();
            str = str.substring(9,10);
            if ((str.equals("+")) || (str.equals("-"))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseLogicalExpression();
                leftExpression = new Ast.Expression.Binary(operator,leftExpression,rightExpression);
            }
            else {
                break;
            }
        }
        return leftExpression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() {
        Ast.Expression leftExpression = parsePrimaryExpression();
        while (peek(Token.Type.OPERATOR)) {
            String str = tokens.get(0).toString();
            str = str.substring(9,10);
            if ((str.equals("*")) || (str.equals("/")) || (str.equals("^"))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseLogicalExpression();
                leftExpression = new Ast.Expression.Binary(operator,leftExpression,rightExpression);
            }
            else {
                break;
            }
        }
        return leftExpression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        Ast.Expression expr = new Ast.Expression.Literal(null);
        if (match(Token.Type.INTEGER)) {
            String literal = tokens.get(-1).getLiteral();
            //match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(new BigInteger(literal));
        }
        else if (match(Token.Type.DECIMAL)) {
            String literal = tokens.get(-1).getLiteral();
            //match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(literal));
        }
        //NEEDS WORKS
        else if(match(Token.Type.IDENTIFIER)){
            String str = tokens.get(-1).getLiteral();
            if (str == "NIL") {
                return new Ast.Expression.Literal(null);
            }
            else if (str == "TRUE") {
                return new Ast.Expression.Literal(true);
            }
            else if (str == "FALSE") {
                return new Ast.Expression.Literal(false);
            }
            return new Ast.Expression.Access(Optional.empty(), str);
        }
        else if(match("(")){
            Ast.Expression expression = parseExpression();
            if(!match(")")){
                throw new ParseException("Expected Closing Parenthesis ",-1);
                //TODO
            }
            return new Ast.Expression.Group(expression);
        }
        else if (match(Token.Type.CHARACTER)) {
            String character = tokens.get(-1).toString();
            char x = character.charAt(11);
            return new Ast.Expression.Literal(x);
        }
        else if (match(Token.Type.STRING)) {
            String str = tokens.get(-1).getLiteral();
            if ((str.charAt(0) == '\"') && (str.charAt(str.length()-1) == '\"')) {
                str = str.substring(1, str.length()-1);
                return new Ast.Expression.Literal(str);
            }
            else {
                throw new ParseException("Invalid String", -1);
            }
        }
        else if (match(Token.Type.OPERATOR)) {
            // do more stuff
            String operator = tokens.get(-1).getLiteral();
            if (operator.equals("(")) {
                match(Token.Type.OPERATOR);
                Ast.Expression expression = parseExpression();


            }
            return new Ast.Expression.Literal(operator);
            /*
            new Ast.Expression.Literal(Token.Type.INTEGER);
            match(Token.Type.OPERATOR);
            Ast.Expression expression = parseExpression();
            match(Token.Type.OPERATOR);
            return expression;
            */
        } else {
            throw new ParseException("Invalid primary expression", -1);
            //TODO
        }
        //return null;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns.equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}