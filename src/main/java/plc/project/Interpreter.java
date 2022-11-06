package plc.project;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.Comparable;
import java.util.function.Function;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args-> {
            if (!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected a BigDecimal, received " +
                        args.get(0).getValue().getClass().getName() + ".");
            }
            BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });
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
/*
    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        return Environment.NIL;
    }*/

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        String name = ast.getName();
        List<Ast.Statement> listStatements = ast.getStatements();
        List<String> listString = ast.getParameters();
        List<Environment.PlcObject> listPlcObj = new ArrayList<>();
        try {
            scope = new Scope(scope);
            for (int i = 0; i < listString.size(); i++) {
                Object x= listString.get(i);
                //visit(listString.get(i));
                Environment.PlcObject plcObj = Environment.create(x);
                System.out.print("HOLDER");
                scope.defineVariable(listString.get(i), true, plcObj);
            }
            for (int i = 0; i < listStatements.size(); i++) {
                Object temp = visit(listStatements.get(i));
                listPlcObj.add(Environment.create(temp));
            }
        } finally {
            //scope.defineFunction(name, listString.size(), Function<<listPlcObj>, Environment.PlcObject>);
            scope = scope.getParent();
        }
        return Environment.NIL;
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
        requireType(Ast.Expression.Access.class, new Environment.PlcObject(scope, expression));
        String name = ((Ast.Expression.Access)expression).getName();
        Optional<Ast.Expression> optional = ((Ast.Expression.Access)expression).getOffset();
        Environment.Variable variable = scope.lookupVariable(name);
        if (optional.isPresent()) {
            Ast.Expression offset = optional.get();
            Ast.Expression value = ast.getValue();
            Environment.PlcObject offsetVal = visit(offset);
            Environment.PlcObject valueVal = visit(value);
            Object temp = variable.getValue().getValue();
            List<BigInteger> list = (List<BigInteger>) temp;
            BigInteger tempOffsetVal = (BigInteger) offsetVal.getValue();
            BigInteger tempObjVal = (BigInteger) valueVal.getValue();
            int index = tempOffsetVal.intValue();
            list.set(index, tempObjVal);
            variable.setValue(Environment.create(list));
            return Environment.NIL;
        }
        else {
            Ast.Expression value = ast.getValue();
            Environment.PlcObject obj = visit(value);
            variable.setValue(Environment.create(obj.getValue()));
            return Environment.NIL;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Environment.PlcObject plcObj = visit(ast.getCondition());
        requireType(Boolean.class, plcObj);
        try {
            scope = new Scope(scope);
            if (plcObj.getValue().equals(Boolean.TRUE)) {
                ast.getThenStatements().forEach(this::visit);
            }
            else {
                ast.getElseStatements().forEach(this::visit);
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject plcObj = visit(ast.getCondition());
        try {
            scope = new Scope(scope);
            List<Ast.Statement.Case> list = ast.getCases();
            Object size = list.size();
            for (int i = 0; i < list.size(); i++) {
                Optional<Ast.Expression> optional = list.get(i).getValue();
                if (optional.isPresent()) {
                    Ast.Expression expression = optional.get();
                    Environment.PlcObject checkObj = visit(expression);
                    if (plcObj.getValue().equals(checkObj.getValue())) {
                        visit(list.get(i));
                        break;
                    }
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        ast.getStatements().forEach(this::visit);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }


    // MIGHT BE COMPLETELY WRONG.. LOL
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Ast.Expression expression = ast.getValue();
        Environment.PlcObject plcObj = visit(expression);
        throw new Return(plcObj);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        String x = ast.toString();
        if (x.contains("literal=null")) {
            return new Environment.PlcObject(scope, Environment.NIL.getValue());
        }
        return new Environment.PlcObject(scope, ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        Ast.Expression expression = ast.getExpression();
        Environment.PlcObject plcObj = visit(expression);
        return plcObj;
    }

    // BINARY = 90% good
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        Ast.Expression leftExpression = ast.getLeft();
        Environment.PlcObject lhs = visit(leftExpression);
        if ((operator.equals("&&")) || (operator.equals("||"))) {
            requireType(Boolean.class, lhs);
            if (operator.equals("||")) {
                if ((Boolean)lhs.getValue() == true) {
                    return new Environment.PlcObject(scope, true);
                }
                Ast.Expression rightExpression = ast.getRight();
                Environment.PlcObject rhs = visit(rightExpression);
                if ((Boolean)rhs.getValue() == true) {
                    return new Environment.PlcObject(scope, true);
                }
                return new Environment.PlcObject(scope, false);
            }
            // else: operator is AND
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (((Boolean)lhs.getValue() != true) || ((Boolean)rhs.getValue() != true))  {
                return new Environment.PlcObject(scope, false);
            }
            return new Environment.PlcObject(scope, true);
        }
        else if ((operator.equals("<")) || (operator.equals(">"))) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            Object returnVal = requireType(Comparable.class, lhs).compareTo(requireType(Comparable.class, rhs));
            if ((returnVal.equals(-1) && operator.equals("<")) || (returnVal.equals(1) && operator.equals(">"))) {
                return new Environment.PlcObject(scope, Boolean.TRUE);
            }
            else {
                return new Environment.PlcObject(scope, Boolean.FALSE);
            }

            //return new Environment.PlcObject(this.scope, returnVal);
            //requireType(Comparable.class, rhs);
            //return Environment.create(requireType(Comparable.class, lhs).)
        }
        else if ((operator.equals("==")) != (operator.equals("!="))) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (operator.equals("==")) {
                if (lhs.getValue().equals(rhs.getValue())) {
                    return new Environment.PlcObject(scope, true);
                }
                return new Environment.PlcObject(scope, false);
            }
            // looking for not equal
            if (lhs.getValue().equals(rhs.getValue())) {
                return new Environment.PlcObject(scope, false);
            }
            return new Environment.PlcObject(scope, true);
        }
        else if (operator.equals("+")) {
            Object x = lhs.getValue();
            String tempStr = x.toString();
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            Object x2 = rhs.getValue();
            String tempStr2 = x2.toString();
            String type = "BigInteger";
            for (int i = 0; i < tempStr.length(); i++) {
                if (((tempStr.charAt(i) > 64) && (tempStr.charAt(i) < 91)) || ((tempStr.charAt(i) > 96) && (tempStr.charAt(i) < 123))) {
                    type = "String";
                    break;
                }
                else if (tempStr.charAt(i) == 46) {
                    type = "BigDecimal";
                    break;
                }
            }
            // check the right hand side bc if either is a string, it concatenates to a string!
            for (int i = 0; i < tempStr2.length(); i++) {
                if (((tempStr2.charAt(i) > 64) && (tempStr2.charAt(i) < 91)) || ((tempStr2.charAt(i) > 96) && (tempStr2.charAt(i) < 123))) {
                    type = "String";
                    break;
                }
            }
            if (type.equals("String")) {
                requireType(String.class, rhs);
                String temp1 = (String) lhs.getValue();
                String temp2 = (String) rhs.getValue();;
                return new Environment.PlcObject(scope, temp1.concat(temp2));
            }
            else if (type.equals("BigInteger"))  {
                requireType(BigInteger.class, rhs);
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();;
                return new Environment.PlcObject(scope, temp1.add(temp2));
            }
            else if (type.equals("BigDecimal"))  {
                requireType(BigDecimal.class, rhs);
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();;
                return new Environment.PlcObject(scope, temp1.add(temp2));
            }
        }
        else if ((operator.equals("-")) || (operator.equals("*"))) {
            Object x = lhs.getValue();
            String tempStr = x.toString();
            String type = "BigInteger";
            for (int i = 0; i < tempStr.length(); i++) {
                if (tempStr.charAt(i) == 46) {
                    type = "BigDecimal";
                    break;
                }
            }
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (type.equals("BigDecimal")) {
                requireType(BigDecimal.class, rhs);
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();
                if (operator.equals("-")) {
                    return new Environment.PlcObject(scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(scope, temp1.multiply(temp2));
            }
            else {
                requireType(BigInteger.class, rhs);
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();
                if (operator.equals("-")) {
                    return new Environment.PlcObject(scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(scope, temp1.multiply(temp2));
            }
        }
        else if (operator.equals("/")) {
            Object x = lhs.getValue();
            String tempStr = x.toString();
            String type = "BigInteger";
            for (int i = 0; i < tempStr.length(); i++) {
                if (tempStr.charAt(i) == 46) {
                    type = "BigDecimal";
                    break;
                }
            }
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (type.equals("BigInteger"))  {
                requireType(BigInteger.class, rhs);
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();;
                if (temp2.equals(0)) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return new Environment.PlcObject(this.scope, temp1.divide(temp2));
            }
            else if (type.equals("BigDecimal")) {
                requireType(BigDecimal.class, rhs);
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();
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
            requireType(BigInteger.class, lhs);
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            requireType(BigInteger.class, rhs);
            BigInteger temp1 = (BigInteger) lhs.getValue();
            int temp2 = (int) rhs.getValue();
            return new Environment.PlcObject(scope, temp1.pow(temp2));
        }
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            Environment.PlcObject obj = visit(expression);
            requireType(BigInteger.class, obj);
            Environment.Variable returnObj = scope.lookupVariable(ast.getName());
            Object temp = returnObj.getValue().getValue();
            List<BigInteger> list = (List<BigInteger>) temp;
            BigInteger tempVal = (BigInteger) obj.getValue();
            int index = tempVal.intValue();
            return new Environment.PlcObject(scope, list.get(index));
        }
        Object temp = scope.lookupVariable(ast.getName()).getValue().getValue();
        return new Environment.PlcObject(scope, temp);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.PlcObject> plcArgumentList = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Environment.PlcObject plcObj = visit(arguments.get(i));
            plcArgumentList.add(plcObj);
        }
        int arity = arguments.size();
        Environment.Function function = scope.lookupFunction(name, arity);
        return function.invoke(plcArgumentList);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> list = ast.getValues();
        List<Object> list2 = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Environment.PlcObject temp = visit(ast.getValues().get(i));
            list2.add(temp.getValue());
        }
        Environment.PlcObject plcObj = new Environment.PlcObject(scope, list2);
        return plcObj;
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
    public static class Return extends RuntimeException {

        public final Environment.PlcObject value;

        public Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}

