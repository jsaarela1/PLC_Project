package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
/*
        scope.defineFunction("logarithm", 1, args-> {
            if (!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected a BigDecimal, received " +
                        args.get(0).getValue().getClass().getName() + ".");
            }
            BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });
*/
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        String name = ast.getName();
        Boolean isMutable = ast.getMutable();
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            scope.defineVariable(name, isMutable, visit(expression));
        }
        else {
            scope.defineVariable(name, isMutable, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();

            // result is a PLC object
            scope.defineVariable(ast.getName(), true, visit(expression));
        }
        else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Ast.Expression expression = ast.getReceiver();
        requireType(Ast.Expression.Access.class, new Environment.PlcObject(this.scope, expression));
        String name = ((Ast.Expression.Access)expression).getName();
        Optional<Ast.Expression> optional = ((Ast.Expression.Access)expression).getOffset();
        Environment.Variable variable = scope.lookupVariable(name);
        if (optional.isPresent()) {
            Ast.Expression offset = optional.get();
            Ast.Expression value = ast.getValue();
            Object offsetValue = ((Ast.Expression.Literal)offset).getLiteral();
            Object objValue = ((Ast.Expression.Literal)value).getLiteral();
            Object temp = variable.getValue().getValue();
            List<BigInteger> list = (List<BigInteger>) temp;
            BigInteger tempOffsetVal = (BigInteger) offsetValue;
            BigInteger tempObjVal = (BigInteger) objValue;
            int index = tempOffsetVal.intValue();
            list.set(index, tempObjVal);
            variable.setValue(new Environment.PlcObject(this.scope, list));
            return Environment.NIL;
        }
        else {
            Ast.Expression value = ast.getValue();
            Object obj = ((Ast.Expression.Literal)value).getLiteral();
            variable.setValue(new Environment.PlcObject(this.scope, obj));
            return Environment.NIL;
        }
    }


    // ---------------------------------------------------------------------
    // -----Will work when I get Ast.Statement.Expression working-----------
    // ---------------------------------------------------------------------
    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Object obj = ((Ast.Expression.Literal)ast.getCondition()).getLiteral();
        Environment.PlcObject plcObj = new Environment.PlcObject(this.scope, obj);
        requireType(Boolean.class, plcObj);
        if (obj.equals(Boolean.TRUE)) {
            List<Ast.Statement> list = ast.getThenStatements();
            for (int i = 0; i < list.size(); i++) {
                visit(list.get(i));
            }
        }
        // if the boolean equals false
        List<Ast.Statement> list = ast.getElseStatements();
        for (int i = 0; i < list.size(); i++) {
            visit(list.get(i));
        }
        return Environment.NIL;
        //throw new UnsupportedOperationException(); //TODO
    }



    // NEEDS FIXING!!!
    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        // not sure what to do about "inside of a new scope"
        Ast.Expression condition = ast.getCondition();
        Object obj = ((Ast.Expression.Literal)ast.getCondition()).getLiteral();
        List<Ast.Statement.Case> list = ast.getCases();
        for (int i = 0; i < list.size(); i++) {
            if (obj.equals(list.get(i))) {
                visit(list.get(i));
            }
        }
        // otherwise it is DEFAULT
        // evaluate default case here

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        String x = ast.toString();
        if (x.contains("literal=null")) {
            return new Environment.PlcObject(this.scope, Environment.NIL.getValue());
        }
        return new Environment.PlcObject(this.scope, ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        Ast.Expression expression = ast.getExpression();
        Environment.PlcObject plcObj = visit(expression);
        return plcObj;
        //throw new UnsupportedOperationException(); //TODO
    }

    // BINARY = 90% good
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Ast.Expression leftExpression = ast.getLeft();
        Object lhs = ((Ast.Expression.Literal)leftExpression).getLiteral();
        if ((operator.equals("&&")) || (operator.equals("||"))) {
            if (!lhs.getClass().getName().equals("java.lang.Boolean")) {
                throw new RuntimeException("Needs to be a boolean");
            }
            //Environment.PlcObject plcObj = new Environment.PlcObject(this.scope, lhs);
            //requireType(Boolean.class, plcObj);
            if (operator.equals("||")) {
                if ((Boolean)lhs == true) {
                    return new Environment.PlcObject(this.scope, true);
                }
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if ((Boolean)rhs == true) {
                    return new Environment.PlcObject(this.scope, true);
                }
                return new Environment.PlcObject(this.scope, false);
            }
            // else: operator is AND
            Ast.Expression rightExpression = ast.getRight();
            Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
            if (((Boolean)lhs != true) || ((Boolean)rhs != true))  {
                return new Environment.PlcObject(this.scope, false);
            }
            return new Environment.PlcObject(this.scope, true);
        }
        else if ((operator.equals("==")) != (operator.equals("!="))) {
            Ast.Expression rightExpression = ast.getRight();
            Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
            if (operator.equals("==")) {
                if (lhs.equals(rhs)) {
                    return new Environment.PlcObject(this.scope, true);
                }
                return new Environment.PlcObject(this.scope, false);
            }
            // looking for not equal
            if (lhs.equals(rhs)) {
                return new Environment.PlcObject(this.scope, false);
            }
            return new Environment.PlcObject(this.scope, true);
        }
        else if (operator.equals("+")) {
            if (lhs.getClass().getName().equals("java.lang.String"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.lang.String")) {
                    throw new RuntimeException("Not matching types");
                }
                String temp1 = (String) lhs;
                String temp2 = (String) rhs;;
                return new Environment.PlcObject(this.scope, temp1.concat(temp2));
            }
            else if (lhs.getClass().getName().equals("java.math.BigInteger"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigInteger")) {
                    throw new RuntimeException("Not matching types");
                }
                BigInteger temp1 = (BigInteger) lhs;
                BigInteger temp2 = (BigInteger) rhs;;
                return new Environment.PlcObject(this.scope, temp1.add(temp2));
            }
            else if (lhs.getClass().getName().equals("java.math.BigDecimal"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigDecimal")) {
                    throw new RuntimeException("Not matching types");
                }
                BigDecimal temp1 = (BigDecimal) lhs;
                BigDecimal temp2 = (BigDecimal) rhs;;
                return new Environment.PlcObject(this.scope, temp1.add(temp2));
            }
        }
        else if ((operator.equals("-")) || (operator.equals("*"))) {
            if (lhs.getClass().getName().equals("java.math.BigDecimal"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigDecimal")) {
                    throw new RuntimeException("Not matching types");
                }
                BigDecimal temp1 = (BigDecimal) lhs;
                BigDecimal temp2 = (BigDecimal) rhs;
                if (operator.equals("-")) {
                    return new Environment.PlcObject(this.scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(this.scope, temp1.multiply(temp2));
            }
        }
        else if (operator.equals("/")) {
            if (lhs.getClass().getName().equals("java.math.BigInteger"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigInteger")) {
                    throw new RuntimeException("Not matching types");
                }
                BigInteger temp1 = (BigInteger) lhs;
                BigInteger temp2 = (BigInteger) rhs;;
                if (temp2.equals(0)) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return new Environment.PlcObject(this.scope, temp1.divide(temp2));
            }
            else if (lhs.getClass().getName().equals("java.math.BigDecimal"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigDecimal")) {
                    throw new RuntimeException("Not matching types");
                }
                BigDecimal temp1 = (BigDecimal) lhs;
                BigDecimal temp2 = (BigDecimal) rhs;
                if (temp2.equals(0)) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return new Environment.PlcObject(this.scope, temp1.divide(temp2, RoundingMode.HALF_EVEN));
            }
        }


        // ------------------------------------------------------------------
        //         MIGHT NEED TO FIX BC OF DOCUMENTATION AB POWERS
        // ------------------------------------------------------------------
        else if (operator.equals("^")) {
            if (lhs.getClass().getName().equals("java.math.BigInteger"))  {
                Ast.Expression rightExpression = ast.getRight();
                Object rhs = ((Ast.Expression.Literal)rightExpression).getLiteral();
                if (!rhs.getClass().getName().equals("java.math.BigInteger")) {
                    throw new RuntimeException("Not matching types");
                }
                BigInteger temp1 = (BigInteger) lhs;
                int temp2 = (int) rhs;
                return new Environment.PlcObject(this.scope, temp1.pow(temp2));
            }
        }
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            Object obj = ((Ast.Expression.Literal)expression).getLiteral();
            if (obj.getClass().getName() != "java.math.BigInteger") {
                throw new RuntimeException("Offset must be a BigInteger");
            }
            Environment.Variable returnObj = scope.lookupVariable(ast.getName());
            Object temp = returnObj.getValue().getValue();
            List<BigInteger> list = (List<BigInteger>) temp;
            BigInteger tempVal = (BigInteger) obj;
            int index = tempVal.intValue();
            return new Environment.PlcObject(this.scope, list.get(index));
        }
        return new Environment.PlcObject(this.scope, ast.getName());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> list = ast.getValues();
        List<Object> list2 = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Ast.Expression.Literal temp = (Ast.Expression.Literal) ast.getValues().get(i);
            list2.add(temp.getLiteral());
        }
        Environment.PlcObject plcObj = new Environment.PlcObject(this.scope, list2);
        return plcObj;
        //throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
