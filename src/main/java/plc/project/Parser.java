package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
        List<Ast.Global> listGlobals = new ArrayList<>();
        List<Ast.Function> listFunctions = new ArrayList<>();
        if (!tokens.has(0)) {
            return new Ast.Source(listGlobals, listFunctions);
        }
        String str = tokens.get(0).getLiteral();
        // list of globals
        while ((str.equals("LIST")) || (str.equals("VAR")) || (str.equals("VAL"))) {
            if ((str.equals("LIST")) || (str.equals("VAR")) || (str.equals("VAL"))) {
                Ast.Global global = parseGlobal();
                listGlobals.add(global);
            }
            if (tokens.has(0)) {
                str = tokens.get(0).getLiteral();
                if (str.equals(";")) {
                    tokens.advance();
                    if (tokens.has(0)) {
                        str = tokens.get(0).getLiteral();
                    }
                }
            }
            else {
                break;
            }
        }
        while (str.equals("FUN")) {
            if (str.equals("FUN")) {
                Ast.Function function = parseFunction();
                listFunctions.add(function);
            }
            if (tokens.has(0)) {
                str = tokens.get(0).getLiteral();
                if (str.equals(";")) {
                    tokens.advance();
                    if (tokens.has(0)) {
                        str = tokens.get(0).getLiteral();
                    }
                }
            }
            else {
                break;
            }
        }
        return new Ast.Source(listGlobals, listFunctions);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        String str = tokens.get(0).getLiteral();
        Ast.Global global;
        if (str.contains("LIST")) {
            global = parseList();
        }
        else if (str.contains("VAR")) {
            global = parseMutable();
        }
        else if (str.contains("VAL")) {
            global = parseImmutable();
        }
        else {
            throw new ParseException("Invalid global ", tokens.index - 1);
        }
        if (tokens.has(0)) {
            str = tokens.get(0).getLiteral();
        }
        else {
            throw new ParseException("Missing semi-colon ", tokens.index - 1);
        }
        if (!str.equals(";")) {
            throw new ParseException("Missing semi-colon ", tokens.index - 1);
        }
        tokens.advance();
        return global;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("LIST")) {
            throw new ParseException("Invalid list ", tokens.index);
        }
        tokens.advance();
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("List must be followed by identifier ", tokens.index - 1);
        }
        String name = tokens.get(-1).getLiteral();
        str = tokens.get(0).getLiteral();
        if (!str.equals("=")) {
            throw new ParseException("Identifier must be followed by an equal sign ", tokens.index - 1);
        }
        tokens.advance();
        str = tokens.get(0).getLiteral();
        if (!str.equals("[")) {
            throw new ParseException("Should be an open bracket ", tokens.index - 1);
        }
        tokens.advance();
        str = tokens.get(0).getLiteral();
        if (str.equals("]")) {
            tokens.advance();
            return new Ast.Global(name, true, Optional.empty());
        }
        List<Ast.Expression> list = new ArrayList<>();
        Ast.Expression expression = parseExpression();
        list.add(expression);
        str = tokens.get(0).getLiteral();
        tokens.advance();
        if (str.equals("]")) {
            return new Ast.Global(name, true, Optional.of(expression));
        }
        while (str.equals(",")) {
            expression = parseExpression();
            list.add(expression);
            str = tokens.get(0).getLiteral();
            tokens.advance();
        }
        if (!str.equals("]")) {
            throw new ParseException("Should be a closed bracket ", tokens.index - 1);
        }
        // not sure how to return a list of elements
        // need to substitute list for expression
        return new Ast.Global(name, true, Optional.of(expression));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("VAR")) {
            throw new ParseException("Invalid val ", tokens.index - 1);
        }
        tokens.advance();
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier must follow VAR ", tokens.index - 1);
        }
        String name = tokens.get(-1).getLiteral();
        // check if there are more tokens, if not return
        if (!tokens.has(1)) {
            return new Ast.Global(name, true, Optional.empty());
        }
        str = tokens.get(0).getLiteral();
        if (!str.equals("=")) {
            throw new ParseException("Invalid operator ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        return new Ast.Global(name, true, Optional.of(expression));
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("VAL")) {
            throw new ParseException("Invalid val ", tokens.index - 1);
        }
        tokens.advance();
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Identifier must follow VAL ", tokens.index - 1);
        }
        String name = tokens.get(-1).getLiteral();
        str = tokens.get(0).getLiteral();
        if (!str.equals("=")) {
            throw new ParseException("Invalid operator ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        return new Ast.Global(name, false, Optional.of(expression));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("FUN")) {
            throw new ParseException("Invalid function ", tokens.index - 1);
        }
        tokens.advance();
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing identifier in function ", tokens.index - 1);
        }
        String strLiteral = tokens.get(-1).getLiteral();
        str = tokens.get(0).getLiteral();
        if (!str.equals("(")) {
            throw new ParseException("Missing opening parenthesis ", tokens.index - 1);
        }
        tokens.advance();
        List<String> parameters = new ArrayList<>();
        if (match(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(-1).toString());
            str = tokens.get(0).getLiteral();
            while (str.equals(",")) {
                tokens.advance();
                if (match(Token.Type.IDENTIFIER)) {
                    parameters.add(tokens.get(-1).toString());
                    str = tokens.get(0).getLiteral();
                }
                else {
                    throw new ParseException("Invalid identifier ", tokens.index - 1);
                }
            }
        }
        else {
            str = tokens.get(0).getLiteral();
        }
        if (!str.equals(")")) {
            throw new ParseException("Missing closing parenthesis ", tokens.index - 1);
        }
        tokens.advance();
        str = tokens.get(0).getLiteral();
        if (!str.equals("DO")) {
            throw new ParseException("Missing DO ", tokens.index - 1);
        }
        tokens.advance();
        List<Ast.Statement> statements = new ArrayList<>();
        statements = parseBlock();
        if ((!tokens.has(0)) || (!tokens.get(0).getLiteral().equals("END"))) {
            throw new ParseException("Invalid END to function ", -1);
        }
        tokens.advance();
        return new Ast.Function(strLiteral, parameters, statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        // need to account for 0-infinity statements being allowed
        List<Ast.Statement> list = new ArrayList<>();
        String str = tokens.get(0).getLiteral();
        while ((tokens.has(0)) && (!((str.equals("END")) || (str.equals("ELSE")) || (str.equals("DEFAULT")) || (str.equals("CASE"))))) {
            Ast.Statement statement = parseStatement();
            list.add(statement);
            if (tokens.has(0)) {
                str = tokens.get(0).getLiteral();
            }
        }
        return list;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (str.contains("LET")) {
            Ast.Statement.Declaration statement = parseDeclarationStatement();
            return statement;
        }
        else if (str.contains("SWITCH")) {
            Ast.Statement.Switch statement = parseSwitchStatement();
            return statement;
        }
        else if (str.contains("IF")) {
            Ast.Statement.If statement = parseIfStatement();
            return statement;
        }
        else if (str.contains("WHILE")) {
            Ast.Statement.While statement = parseWhileStatement();
            return statement;
        }
        else if (str.contains("RETURN")) {
            Ast.Statement.Return statement = parseReturnStatement();
            return statement;
        }
        Ast.Expression leftExpression = parseExpression();
        Ast.Statement statement = new Ast.Statement.Expression(leftExpression);
        while (peek(Token.Type.OPERATOR)) {
            String str1 = tokens.get(0).getLiteral();
            if (str1.equals("=")) {
                match(Token.Type.OPERATOR);
                Ast.Expression rightExpression = parseExpression();
                statement = new Ast.Statement.Assignment(leftExpression, rightExpression);
            }
            else {
                break;
            }
        }
        if ((!tokens.has(0)) || (!tokens.get(0).getLiteral().equals(";"))) {
            throw new ParseException("Needs a semi-colon at the end ", tokens.index - 1);
        }
        tokens.advance();
        return statement;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("LET")) {
            throw new ParseException("Invalid Let statement ", tokens.index - 1);
        }
        tokens.advance();
        match(Token.Type.IDENTIFIER);
        String strLiteral = tokens.get(-1).getLiteral();
        if (!tokens.has(0)) {
            throw new ParseException("Missing semicolon ", tokens.index - 1);
        }
        str = tokens.get(0).getLiteral();
        if (str.equals("=")) {
            tokens.advance();
            Ast.Expression expression = parseExpression();
            str = tokens.get(0).getLiteral();
            if (str.equals(";")) {
                return new Ast.Statement.Declaration(strLiteral, Optional.of(expression));
            }
            throw new ParseException("Missing semicolon ", tokens.index - 1);
        }
        else if (str.equals(";")) {
            return new Ast.Statement.Declaration(strLiteral, Optional.empty());
        }
        throw new ParseException("Invalid declarative statement ", tokens.index - 1);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("IF")) {
            throw new ParseException("Not if statement ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        str = tokens.get(0).getLiteral();
        if (!str.equals("DO")) {
            throw new ParseException("Missing DO in statement", tokens.index - 1);
        }
        tokens.advance();
        List<Ast.Statement> thenList = new ArrayList<>();
        List<Ast.Statement> elseList = new ArrayList<>();
        thenList = parseBlock();
        if (!tokens.has(0)) {
            throw new ParseException("Missing end statement ", tokens.index - 1);
        }
        str = tokens.get(0).getLiteral();
        if (str.equals("ELSE")) {
            tokens.advance();
            elseList = parseBlock();
        }
        else if (!str.equals("END")) {
            throw new ParseException("Error missing END ", tokens.index - 1);
        }
        Ast.Statement.If statement = new Ast.Statement.If(expression, thenList, elseList);
        return statement;
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.contains("SWITCH")) {
            throw new ParseException("Not switch statement ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();
        str = tokens.get(0).getLiteral();
        while (str.equals("CASE")) {
            Ast.Statement.Case caseStatement = parseCaseStatement();
            cases.add(caseStatement);
            if (tokens.has(0)) {
                str = tokens.get(0).getLiteral();
            }
            else {
                throw new ParseException("Invalid case statement ", tokens.index - 1);
            }
        }
        if (!str.equals("DEFAULT")) {
            throw new ParseException("Invalid Default statement ", tokens.index - 1);
        }
        tokens.advance();
        List<Ast.Statement> defaultCases = new ArrayList<>();
        defaultCases = parseBlock();
        if (defaultCases.size() == 0) {
            throw new ParseException("Missing DEFAULT statement ", tokens.index - 1);
        }
        if (!tokens.has(0)) {
            throw new ParseException("Missing END statement ", tokens.index - 1);
        }
        str = tokens.get(0).getLiteral();
        if (!str.equals("END")) {
            throw new ParseException("Invalid end statement ", tokens.index - 1);
        }
        Ast.Statement.Switch statement = new Ast.Statement.Switch(expression, cases);
        return statement;
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        List<Ast.Statement> statements = new ArrayList<>();
        if (str.equals("CASE")) {
            tokens.advance();
            Ast.Expression expression = parseExpression();
            str = tokens.get(0).getLiteral();
            if (!str.equals(":")) {
                throw new ParseException("Missing colon in case statement ", tokens.index - 1);
            }
            tokens.advance();
            statements = parseBlock();
            Ast.Statement.Case caseStatement = new Ast.Statement.Case(Optional.of(expression), statements);
            return caseStatement;
        }
        throw new ParseException("Invalid case statement ", tokens.index - 1); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("WHILE")) {
            throw new ParseException("Not WHILE statement ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        str = tokens.get(0).getLiteral();
        if (!str.equals("DO")) {
            throw new ParseException("Missing DO in statement", tokens.index - 1);
        }
        tokens.advance();
        List<Ast.Statement> whileList = new ArrayList<>();
        whileList = parseBlock();
        if (!tokens.has(0)) {
            throw new ParseException("Missing end statement ", tokens.index - 1);
        }
        str = tokens.get(0).getLiteral();
        if (!str.equals("END")) {
            throw new ParseException("Error missing END ", tokens.index - 1);
        }
        Ast.Statement.While statement = new Ast.Statement.While(expression, whileList);
        return statement;
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        String str = tokens.get(0).getLiteral();
        if (!str.equals("RETURN")) {
            throw new ParseException("Not RETURN statement ", tokens.index - 1);
        }
        tokens.advance();
        Ast.Expression expression = parseExpression();
        if (!tokens.has(0)) {
            throw new ParseException("Missing semicolon ", tokens.index - 1);
        }
        str = tokens.get(0).getLiteral();
        if (!str.equals(";")) {
            throw new ParseException("Missing semicolon to return statement", tokens.index - 1);
        }
        Ast.Statement.Return statement = new Ast.Statement.Return(expression);
        return statement;
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
            String str = tokens.get(0).getLiteral();
            if ((str.equals("&&")) || (str.equals("||"))) {
                match(Token.Type.OPERATOR);
                String operator = tokens.get(-1).getLiteral();
                Ast.Expression rightExpression = parseComparisonExpression();
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
                Ast.Expression rightExpression = parseAdditiveExpression();
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
                Ast.Expression rightExpression = parseMultiplicativeExpression();
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
                Ast.Expression rightExpression = parsePrimaryExpression();
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
            return new Ast.Expression.Literal(new BigInteger(literal));
        }
        else if (match(Token.Type.DECIMAL)) {
            String literal = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(new BigDecimal(literal));
        }
        else if (match(Token.Type.CHARACTER)) {
            String character = tokens.get(-1).getLiteral();
            if (character.length() == 3) {
                char x = character.charAt(1);
                return new Ast.Expression.Literal(x);
            }
            character = character.substring(1,4);
            return new Ast.Expression.Literal(character);
        }
        else if (match(Token.Type.STRING)) {
            String str = tokens.get(-1).getLiteral();
            if ((str.charAt(0) == '\"') && (str.charAt(str.length()-1) == '\"')) {
                str = str.substring(1, str.length()-1);
                for (int i = 1; i < str.length() - 1; i++) {
                    if ((str.charAt(i) == '\'') && (str.charAt(i) == '\'')) {
                        str.replace("\\\\", "\\");
                    }
                }
                return new Ast.Expression.Literal(str);
            }
            else {
                throw new ParseException("Invalid String", tokens.index - 1);
            }
        }
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
            if (tokens.has(1)) {
                String operator1 = tokens.get(0).toString();
                operator1 = operator1.substring(9,10);
                if (operator1.equals("[")) {
                    match(Token.Type.OPERATOR);
                    if (tokens.get(0).toString().substring(9,10).equals("]")) {
                        tokens.advance();
                        return new Ast.Expression.Access(Optional.empty(), str);
                    }
                    Ast.Expression expression2 = parseExpression();
                    String operator2 = tokens.get(0).toString();
                    operator2 = operator2.substring(9,10);
                    if (operator2.equals("]")) {
                        tokens.advance();
                        Ast.Expression.Access accessExpression = new Ast.Expression.Access(Optional.of(expression2), str);
                        return accessExpression;
                    }
                }
                else if (operator1.equals("(")) {
                    match(Token.Type.OPERATOR);
                    List<Ast.Expression> list = new ArrayList<>();
                    String operator2 = tokens.get(0).toString();
                    operator2 = operator2.substring(9,10);
                    if (operator2.equals(")")) {
                        tokens.advance();
                        return new Ast.Expression.Function(str, list);
                    }
                    Ast.Expression expression2 = parseExpression();
                    list.add(expression2);
                    operator2 = tokens.get(0).toString();
                    operator2 = operator2.substring(9,10);
                    while (operator2.equals(",")) {
                        match(Token.Type.OPERATOR);
                        Ast.Expression tempExpression = parseExpression();
                        list.add(tempExpression);
                        operator2 = tokens.get(0).toString();
                        operator2 = operator2.substring(9,10);
                    }
                    if (operator2.equals(")")) {
                        tokens.advance();
                        Ast.Expression.Function function = new Ast.Expression.Function(str, list);
                        return function;
                    }
                }
            }
            return new Ast.Expression.Access(Optional.empty(), str);
        }
        else if (match(Token.Type.OPERATOR)) {
            String operator = tokens.get(-1).getLiteral();
            if (operator.equals("(")) {
                String str = tokens.get(0).getLiteral();
                Ast.Expression expression = parseExpression();
                if (match(Token.Type.OPERATOR)) {
                    operator = tokens.get(-1).getLiteral();
                    if (operator.equals(")")) {
                        Ast.Expression.Group group = new Ast.Expression.Group(expression);
                        return group;
                    }
                }
            }
        }
        throw new ParseException("Invalid primary expression", tokens.index - 1);
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