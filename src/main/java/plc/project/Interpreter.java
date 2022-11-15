package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.Comparable;

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


        scope.defineFunction("converter", 2, args -> {
            String number = new String();
            int i, n = 0;
            ArrayList<BigInteger> quotients = new ArrayList<BigInteger>();
            ArrayList<BigInteger> remainders = new ArrayList<BigInteger>();

            BigInteger base10 = requireType(
                                        BigInteger.class,
                                        Environment.create(args.get(0).getValue())
            );

            BigInteger base = requireType(
                    BigInteger.class,
                    Environment.create(args.get(1).getValue())
            );

            do {
                quotients.add(quotients.get(n).divide(base));
                remainders.add(quotients.get(n).subtract((quotients.get(n+1).multiply(base))));
            } while (quotients.get(n).compareTo(BigInteger.ZERO) > 0);

            for (i = 0; i < remainders.size(); i++) {
                number = remainders.get(i).toString() + number;
            }

            return Environment.create(number);
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

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        String name = ast.getName();
        List<Ast.Statement> listStatements = ast.getStatements();
        List<String> listString = ast.getParameters();
        List<Environment.PlcObject> listPlcObj = new ArrayList<>();
        Scope originalScope = scope;

        scope.defineFunction(name, listString.size(), args -> {
            scope = new Scope(scope.getParent());
            for (int i = 0; i < listString.size(); i++) {
                scope.defineVariable(listString.get(i), true, Environment.create(args.get(i).getValue()));
            }

            for (int i = 0; i < listStatements.size(); i++) {
                if (listStatements.get(i).getClass().equals(Ast.Statement.Return.class)) {
                    try {
                        visit(listStatements.get(i));
                    } catch (RuntimeException t) {
                        String x2 = t.getLocalizedMessage();
                        int y2 = t.toString().length();
                        for (int j = 0; j < y2; j++) {
                            char x = t.toString().charAt(j);
                            String y = t.getMessage();
                        }
                        String y = t.getMessage();
                        String x= t.toString();
                        if (t.getClass().equals(Return.class)) {
                            String x4= t.toString();
                        }

                        System.out.println("3");
                        //return t.fillInStackTrace();
                        //return Return.value;
                    }
                }
            }
            return Environment.NIL;
        });


        listPlcObj.add(new Environment.PlcObject(scope, BigInteger.TEN));
        try {
            scope = new Scope(originalScope);
            Environment.Function function = scope.lookupFunction(name, listString.size());
            Environment.Function function2 = scope.lookupFunction(name, listString.size());
            function.invoke(listPlcObj);
        } finally {
            scope = originalScope.getParent();
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
                requireType(Boolean.class, rhs);
                if ((Boolean)rhs.getValue() == true) {
                    return new Environment.PlcObject(scope, true);
                }
                return new Environment.PlcObject(scope, false);
            }
            // else: operator is AND
            if ((Boolean)lhs.getValue() != true)  {
                return new Environment.PlcObject(scope, false);
            }
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            requireType(Boolean.class, rhs);
            if ((Boolean)rhs.getValue() != true)  {
                return new Environment.PlcObject(scope, false);
            }
            return new Environment.PlcObject(scope, true);
        }
        else if ((operator.equals("<")) || (operator.equals(">"))) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (!lhs.getValue().getClass().equals(rhs.getValue().getClass())) {
                throw new RuntimeException("Both expressions must be of the same class type");
            }
            Object returnVal = requireType(Comparable.class, lhs).compareTo(requireType(Comparable.class, rhs));
            if ((returnVal.equals(-1) && operator.equals("<")) || (returnVal.equals(1) && operator.equals(">"))) {
                return new Environment.PlcObject(scope, Boolean.TRUE);
            }
            else {
                return new Environment.PlcObject(scope, Boolean.FALSE);
            }
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
            if (lhs.getValue().getClass().equals(String.class) || (rhs.getValue().getClass().equals(String.class))) {
                String temp1 = lhs.getValue().toString();
                String temp2 = rhs.getValue().toString();
                return new Environment.PlcObject(scope, temp1.concat(temp2));
            }
            else {
                if (!lhs.getValue().getClass().equals(rhs.getValue().getClass())) {
                    throw new RuntimeException("Both expressions must be of the same class type");
                }
                if (lhs.getValue().getClass().equals(BigInteger.class)) {
                    BigInteger temp1 = (BigInteger) lhs.getValue();
                    BigInteger temp2 = (BigInteger) rhs.getValue();;
                    return new Environment.PlcObject(scope, temp1.add(temp2));
                }
                else if (lhs.getValue().getClass().equals(BigDecimal.class)) {
                    BigDecimal temp1 = (BigDecimal) lhs.getValue();
                    BigDecimal temp2 = (BigDecimal) rhs.getValue();;
                    return new Environment.PlcObject(scope, temp1.add(temp2));
                }
            }
        }
        else if ((operator.equals("-")) || (operator.equals("*"))) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (!lhs.getValue().getClass().equals(rhs.getValue().getClass())) {
                throw new RuntimeException("Both expressions must be of the same class type");
            }
            if (lhs.getValue().getClass().equals(BigInteger.class)) {
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();
                if (operator.equals("-")) {
                    return new Environment.PlcObject(scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(scope, temp1.multiply(temp2));
            }
            else if (lhs.getValue().getClass().equals(BigDecimal.class)) {
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();
                if (operator.equals("-")) {
                    return new Environment.PlcObject(scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(scope, temp1.multiply(temp2));
            }
        }
        else if (operator.equals("/")) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (!lhs.getValue().getClass().equals(rhs.getValue().getClass())) {
                throw new RuntimeException("Both expressions must be of the same class type");
            }
            if (lhs.getValue().getClass().equals(BigInteger.class)) {
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();;
                if (temp2.equals(BigInteger.ZERO)) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return new Environment.PlcObject(this.scope, temp1.divide(temp2));
            }
            else if (lhs.getValue().getClass().equals(BigDecimal.class)) {
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();
                if (temp2.equals(new BigDecimal("0.0"))) {
                    throw new RuntimeException("Cannot divide by zero");
                }
                return new Environment.PlcObject(this.scope, temp1.divide(temp2, RoundingMode.HALF_EVEN));
            }
        }
        else if (operator.equals("^")) {
            requireType(BigInteger.class, lhs);
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            requireType(BigInteger.class, rhs);
            BigInteger temp1 = (BigInteger) lhs.getValue();
            BigInteger temp2 = (BigInteger) rhs.getValue();
            int exp = temp2.intValue();
            if (exp < 1) {
                return new Environment.PlcObject(scope, BigInteger.ZERO);
            }
            return new Environment.PlcObject(scope, temp1.pow(exp));
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

