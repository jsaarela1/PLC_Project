package plc.project;

import javafx.beans.binding.DoubleExpression;
import org.omg.CORBA.Any;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Struct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        throw new UnsupportedOperationException();  // TODO
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

        // do i have these backwards??
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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
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
        throw new UnsupportedOperationException();  // TODO
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


    // no test cases smh.. so no clue atm
    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        Object o = ast.getExpression().getClass();
        if (!ast.getExpression().getClass().equals(Ast.Expression.Binary.class)) {
            throw new RuntimeException("A group expression must be a binary expresssion");
        }
        throw new UnsupportedOperationException();  // TODO
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
        Environment.Variable variable = scope.lookupVariable(name);
        ast.setVariable(variable);
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            visit(expression);
            if (!expression.getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset value must be an integer");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> listOfValues = ast.getValues();
        for (int i = 0; i < listOfValues.size(); i++) {
            Object o = listOfValues.get(i);
            System.out.print(i);
        }
        throw new UnsupportedOperationException();  // TODO
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
