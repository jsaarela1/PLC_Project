package plc.project;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
            Environment.PlcObject offsetVal = visit(offset);
            Environment.PlcObject valueVal = visit(value);
            //Object offsetValue = ((Ast.Expression.Literal)offset).getLiteral();
            //Object objValue = ((Ast.Expression.Literal)value).getLiteral();
            Object temp = variable.getValue().getValue();
            List<BigInteger> list = (List<BigInteger>) temp;
            BigInteger tempOffsetVal = (BigInteger) offsetVal.getValue();
            BigInteger tempObjVal = (BigInteger) valueVal.getValue();
            int index = tempOffsetVal.intValue();
            list.set(index, tempObjVal);
            variable.setValue(new Environment.PlcObject(this.scope, list));
            return Environment.NIL;
        }
        else {
            Ast.Expression value = ast.getValue();
            Environment.PlcObject obj = visit(value);
            Object obj2 = obj.getValue();
            variable.setValue(new Environment.PlcObject(this.scope, obj2));
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
    }



    // NEEDS FIXING!!!
    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        /*
        // not sure what to do about "inside of a new scope"
        Ast.Expression condition = ast.getCondition();
        Environment.PlcObject obj = visit(condition);
        //Object obj = ((Ast.Expression.Literal)ast.getCondition()).getLiteral();
        List<Ast.Statement.Case> list = ast.getCases();
        for (int i = 0; i < list.size(); i++) {
            Object x = obj.getValue();
            Object y = list.get(i).getValue();
           // Object z = y.getValue();
            if (obj.getValue().equals(list.get(i))) {
                visit(list.get(i));
            }
        }*/
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

        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
        /*
        Ast.Expression expression = ast.getCondition();
        Environment.PlcObject obj = visit(expression);
        requireType(Boolean.class, obj);
        while (obj.getValue().equals(Boolean.TRUE)) {
            List<Ast.Statement> list = ast.getStatements();
            for (int i = 0; i < list.size(); i++) {
                Ast.Statement statement= list.get(i);
                visit(statement);
            }
            obj = visit(expression);
        }
        return Environment.NIL;*/
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
                    return new Environment.PlcObject(this.scope, true);
                }
                Ast.Expression rightExpression = ast.getRight();
                Environment.PlcObject rhs = visit(rightExpression);
                if ((Boolean)rhs.getValue() == true) {
                    return new Environment.PlcObject(this.scope, true);
                }
                return new Environment.PlcObject(this.scope, false);
            }
            // else: operator is AND
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
            if (((Boolean)lhs.getValue() != true) || ((Boolean)rhs.getValue() != true))  {
                return new Environment.PlcObject(this.scope, false);
            }
            return new Environment.PlcObject(this.scope, true);
        }

        else if ((operator.equals("==")) != (operator.equals("!="))) {
            Ast.Expression rightExpression = ast.getRight();
            Environment.PlcObject rhs = visit(rightExpression);
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
                return new Environment.PlcObject(this.scope, temp1.concat(temp2));
            }
            else if (type.equals("BigInteger"))  {
                requireType(BigInteger.class, rhs);
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();;
                return new Environment.PlcObject(this.scope, temp1.add(temp2));
            }
            else if (type.equals("BigDecimal"))  {
                requireType(BigDecimal.class, rhs);
                BigDecimal temp1 = (BigDecimal) lhs.getValue();
                BigDecimal temp2 = (BigDecimal) rhs.getValue();;
                return new Environment.PlcObject(this.scope, temp1.add(temp2));
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
                    return new Environment.PlcObject(this.scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(this.scope, temp1.multiply(temp2));
            }
            else {
                requireType(BigInteger.class, rhs);
                BigInteger temp1 = (BigInteger) lhs.getValue();
                BigInteger temp2 = (BigInteger) rhs.getValue();
                if (operator.equals("-")) {
                    return new Environment.PlcObject(this.scope, temp1.subtract(temp2));
                }
                return new Environment.PlcObject(this.scope, temp1.multiply(temp2));
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
            return new Environment.PlcObject(this.scope, temp1.pow(temp2));
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
        Object temp = scope.lookupVariable(ast.getName()).getValue().getValue();
        return new Environment.PlcObject(this.scope, temp);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.PlcObject> list = new ArrayList<>();
        //Function<List<Environment.PlcObject>, Environment.PlcObject> functionList = new Function<>();
        //Environment.Function function = new Environment.Function(name, arguments.size(), list);
        // check lecture for when he mentioned how to do this!
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
