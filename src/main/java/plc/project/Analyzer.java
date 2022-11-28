package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    public Environment.Type getReturnType(String str) {
        Environment.Type returnType = Environment.Type.NIL;
        if (str.equals("Any")) {
            returnType = Environment.Type.ANY;
        }
        else if (str.equals("Comparable")) {
            returnType = Environment.Type.COMPARABLE;
        }
        else if (str.equals("Integer")) {
            returnType = Environment.Type.INTEGER;
        }
        else if (str.equals("Decimal")) {
            returnType = Environment.Type.DECIMAL;
        }
        else if (str.equals("Boolean")) {
            returnType = Environment.Type.BOOLEAN;
        }
        else if (str.equals("Character")) {
            returnType = Environment.Type.CHARACTER;
        }
        else if (str.equals("String")) {
            returnType = Environment.Type.STRING;
        }
        else {
            throw new RuntimeException("Type cannot be unknown");
        }
        return returnType;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        boolean hasMain = false;
        for (Ast.Function function : ast.getFunctions()) {
            String name = function.getName();
            if (function.getName().equals("main")) {
                if (function.getParameters().size() == 0) {
                    hasMain = true;
                    Optional<String> optional = function.getReturnTypeName();
                    if (optional.isPresent()) {
                        String str = optional.get();
                        if (!str.equals("Integer")) {
                            throw new RuntimeException("Return type must be of type Integer");
                        }
                    }
                    else {
                        throw new RuntimeException("Return type must exist and be of type Integer");
                    }
                }
            }
            visit(function);
        }
        if (!hasMain) {
            throw new RuntimeException("A main function does not exist");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        Boolean mutable = ast.getMutable();
        String name = ast.getName();
        String typeStr = ast.getTypeName();
        Environment.Type typeVariable = getReturnType(typeStr);
        scope.defineVariable("type", "type", typeVariable, mutable, Environment.NIL);
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
            if (!expression.getClass().equals(Ast.Expression.PlcList.class)) {
                Environment.Type typeOfValue = expression.getType();
                requireAssignable(typeVariable, typeOfValue);
            }
        }
        Environment.Variable variable = scope.defineVariable(name, name, typeVariable, mutable, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String name = ast.getName();
        List<String> listStr = ast.getParameters();
        List<String> parameterTypeNames = ast.getParameterTypeNames();
        List<Environment.Type> parameterTypeList = new ArrayList<>();
        for (int i = 0; i < parameterTypeNames.size(); i++) {
            Environment.Type parameterType = getReturnType(parameterTypeNames.get(i));
            parameterTypeList.add(parameterType);
        }
        Environment.Type returnType = Environment.Type.NIL;
        Optional<String> optional = ast.getReturnTypeName();
        if (optional.isPresent()) {
            String typeStr = optional.get();
            returnType = getReturnType(typeStr);
        }
        Environment.Function function = scope.defineFunction(name, name, parameterTypeList, returnType, args -> Environment.NIL);
        ast.setFunction(function);

        // visit statements
        try {
            scope = new Scope(scope);
            for (int i = 0; i < parameterTypeNames.size(); i++) {
                scope.defineVariable(listStr.get(i), parameterTypeNames.get(i), parameterTypeList.get(i), true, Environment.NIL);
            }
            scope.defineVariable("returnType", "returnType", returnType, true, Environment.NIL);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);
        if (!expression.getClass().equals(Ast.Expression.Function.class)) {
            throw new RuntimeException("Receiver must be an access expression");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        Environment.Type type = Environment.Type.NIL;
        boolean variableType = false;

        // type of variable
        Optional<String> optionalStr = ast.getTypeName();
        if (optionalStr.isPresent()) {
            String typeStr = optionalStr.get();
            type = getReturnType(typeStr);
            variableType = true;
        }

        // value of variable
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
            // throw runtime exception if the value, if present, is not assignable to the variable
            if (variableType) {
                requireAssignable(type, expression.getType());
            }
            //Environment.Variable variable = scope.defineVariable(name, name, ENv)
            if (!variableType) {
                type = expression.getType();
                variableType = true;
            }
        }
        if (!variableType) {
            throw new RuntimeException("Neither variable type nor type of value are present");
        }

        // define variable in scope, and set variable
        Environment.Variable variable = scope.defineVariable(name, name, type, true, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Ast.Expression value = ast.getValue();
        visit(receiver);
        if (!receiver.getClass().equals(Ast.Expression.Access.class)) {
            throw new RuntimeException("Receiver must be an access expression");
        }
        visit(value);
        requireAssignable(value.getType(), receiver.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("ThenStatements list cannot be empty");
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        Ast.Expression condition = ast.getCondition();
        visit(condition);
        Environment.Type conditionType = condition.getType();
        List<Ast.Statement.Case> cases = ast.getCases();
        // check all cases except the default
        for (int i = 0; i < cases.size(); i++) {
            visit(cases.get(i));
            Optional<Ast.Expression> optional = cases.get(i).getValue();
            if (optional.isPresent()) {
                // ensure the default case does not have a value
                if (i == cases.size() - 1) {
                    throw new RuntimeException("Default case cannot contain a value");
                }
                Ast.Expression expression = optional.get();
                visit(expression);
                requireAssignable(expression.getType(), conditionType);
            }

        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        Ast.Expression expression = ast.getValue();
        visit(expression);
        Environment.Variable returnVariable = scope.lookupVariable("returnType");
        requireAssignable(returnVariable.getType(), expression.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal.equals(Environment.NIL)) {
            ast.setType(Environment.Type.NIL);
        }
        else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (literal instanceof BigInteger) {
            BigInteger value = (BigInteger) literal;
            if (value.compareTo(new BigInteger(String.valueOf(Integer.MAX_VALUE))) == 1) {
                throw new RuntimeException("Value bigger than allowed for a int");
            }
            else if (value.compareTo(new BigInteger(String.valueOf(Integer.MIN_VALUE))) == -1) {
                throw new RuntimeException("Value smaller than allowed for a int");
            }
            ast.setType(Environment.Type.INTEGER);
            return null;
        }

        // BIG DECIMAL MIGHT NEED FIXING !!!
        else {
            BigDecimal value = (BigDecimal)literal;
            BigDecimal vla = new BigDecimal(String.valueOf(Double.MAX_VALUE));
            if (value.compareTo(new BigDecimal(String.valueOf(Double.MAX_VALUE))) == 1) {
                throw new RuntimeException("Value bigger than allowed for a double");
            }
            else if (value.compareTo(new BigDecimal(String.valueOf(Double.MIN_VALUE))) == -1) {
                throw new RuntimeException("Value smaller than allowed for a double");
            }
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }
        return null;
    }


    // no test cases smh... so no clue atm
    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if (!ast.getExpression().getClass().equals(Ast.Expression.Binary.class)) {
            throw new RuntimeException("A group expression must be a binary expresssion");
        }
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Ast.Expression leftExpression = ast.getLeft();
        visit(leftExpression);
        Ast.Expression rightExpression = ast.getRight();
        visit(rightExpression);
        if ((operator.equals("&&")) || (operator.equals("||"))) {
            requireAssignable(Environment.Type.BOOLEAN, leftExpression.getType());
            requireAssignable(Environment.Type.BOOLEAN, rightExpression.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if ((operator.equals("<")) || (operator.equals(">")) || (operator.equals("==")) || (operator.equals("!="))) {
            requireAssignable(Environment.Type.COMPARABLE, leftExpression.getType());
            requireAssignable(Environment.Type.COMPARABLE, rightExpression.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (operator.equals("+")) {
            if ((leftExpression.getType().equals(Environment.Type.STRING)) || (rightExpression.getType().equals(Environment.Type.STRING))) {
                ast.setType(Environment.Type.STRING);
            }
            else if (leftExpression.getType().equals(Environment.Type.INTEGER)) {
                if (!rightExpression.getType().equals(Environment.Type.INTEGER)) {
                    throw new RuntimeException("RHS must be same type as LHS");
                }
                ast.setType(Environment.Type.INTEGER);
            }
            else if (leftExpression.getType().equals(Environment.Type.DECIMAL)) {
                if (!rightExpression.getType().equals(Environment.Type.DECIMAL)) {
                    throw new RuntimeException("RHS must be same type as LHS");
                }
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        else if ((operator.equals("-")) || (operator.equals("*")) || (operator.equals("/"))) {
            if (leftExpression.getType().equals(Environment.Type.INTEGER)) {
                if (!rightExpression.getType().equals(Environment.Type.INTEGER)) {
                    throw new RuntimeException("RHS must be same type as LHS");
                }
                ast.setType(Environment.Type.INTEGER);
            }
            if (leftExpression.getType().equals(Environment.Type.DECIMAL)) {
                if (!rightExpression.getType().equals(Environment.Type.DECIMAL)) {
                    throw new RuntimeException("RHS must be same type as LHS");
                }
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        else if (operator.equals("^")) {
            requireAssignable(Environment.Type.INTEGER, leftExpression.getType());
            requireAssignable(Environment.Type.INTEGER, rightExpression.getType());
            ast.setType(Environment.Type.INTEGER);
        }
        return null;
    }


    // not sure if this works with an offset
    @Override
    public Void visit(Ast.Expression.Access ast) {
        String name = ast.getName();
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
            if (!expression.getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset value must be an integer");
            }
        }
        Environment.Variable variable = scope.lookupVariable(name);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> listArgs = ast.getArguments();
        Environment.Function function = scope.lookupFunction(name, listArgs.size());
        ast.setFunction(function);

        // check that provided arguments are assignable to the corresponding paramater types
        List<Environment.Type> listParameterTypes = function.getParameterTypes();
        for (int i = 0; i < listParameterTypes.size(); i++) {
            visit(listArgs.get(i));
            requireAssignable(listParameterTypes.get(i), listArgs.get(i).getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> listOfValues = ast.getValues();
        Environment.Variable variable = scope.lookupVariable("type");
        Environment.Type listType = variable.getType();
        for (int i = 0; i < listOfValues.size(); i++) {
            Ast.Expression tempValue = listOfValues.get(i);
            visit(tempValue);
            requireAssignable(listType, tempValue.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(Environment.Type.ANY)) {        }
        else if (target.equals(Environment.Type.COMPARABLE)) {
            if (!((type.equals(Environment.Type.INTEGER)) || (type.equals(Environment.Type.DECIMAL)) || (type.equals(Environment.Type.CHARACTER)) || (type.equals(Environment.Type.STRING)))) {
                throw new RuntimeException("Target type does not match the type being used/assigned");
            }
        }
        else {
            if (!target.equals(type)) {
                throw new RuntimeException("Target type does not match the type being used/assigned");
            }
        }
    }

}
