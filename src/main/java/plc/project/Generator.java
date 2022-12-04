package plc.project;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.List;
import java.math.BigInteger;
import java.math.BigDecimal;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        newline(0);
        List<Ast.Global> globalList = ast.getGlobals();
        for (Ast.Global global : globalList) {
            newline(1);
            visit(global);
        }
        if (!globalList.isEmpty()) {
            newline(0);
        }
        newline(1);
        writer.write("public static void main(String[] args) {");
        newline(2);
        writer.write("System.exit(new Main().main());");
        newline(1);
        writer.write("}");
        List<Ast.Function> functionList = ast.getFunctions();
        if (!functionList.isEmpty()) {
            newline(0);
            newline(1);
        }
        for (Ast.Function function : functionList) {
            visit(function);
        }
        newline(0);
        newline(0);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        Environment.Variable variable = ast.getVariable();
        boolean mutable = variable.getMutable();
        Optional<Ast.Expression> optional = ast.getValue();
        if (variable.getJvmName().equals("list")) {
            String typeName = ast.getTypeName();
            if (typeName.equals("Decimal")) {
                writer.write("double[] ");
            }
            else if (typeName.equals("Integer")) {
                writer.write("int[] ");
            }
            else if (typeName.equals("Character")) {
                writer.write("char[] ");
            }
            else if (typeName.equals("String")) {
                writer.write("string[] ");
            }
            print(variable.getName());
            writer.write(" = ");
            if (optional.isPresent()) {
                visit(optional.get());
            }
            writer.write(";");
        }
        // mutable
        else if (mutable) {
            print(variable.getType(), " ", variable.getName());
            if (optional.isPresent()) {
                print(" = ", optional.get());
            }
            writer.write(";");
        }
        // immutable
        else {
            writer.write("final ");
            print(variable.getType(), " ", variable.getName());
            if (optional.isPresent()) {
                print(" = ", optional.get());
            }
            writer.write(";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        Environment.Function function = ast.getFunction();
        print(function.getReturnType().getJvmName());
        writer.write(" ");
        print(function.getName());
        List<String> parameterList = ast.getParameters();
        List<Environment.Type> parameterTypes = function.getParameterTypes();
        writer.write("(");
        for (int i = 0; i < parameterList.size(); i++) {
            print(parameterTypes.get(i));
            writer.write(" ");
            print(parameterList.get(i));
            if ((i+1) != parameterList.size()) {
                writer.write(", ");
            }
        }
        writer.write(") {");
        List<Ast.Statement> statementList = ast.getStatements();
        if (statementList.isEmpty()) {
            writer.write("}");
            return null;
        }
        for (int i = 0; i < statementList.size(); i++) {
            newline(2);
            visit(statementList.get(i));
        }
        newline(1);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        Optional<Ast.Expression> optional = ast.getValue();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            writer.write(" = ");
            visit(expression);
        }
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        visit(receiver);
        writer.write(" = ");
        Ast.Expression value = ast.getValue();
        visit(value);
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");
        newline(0);
        List<Ast.Statement> thenStatements = ast.getThenStatements();
        for (Ast.Statement statement : thenStatements) {
            writer.write("    ");
            visit(statement);
            newline(0);
        }
        List<Ast.Statement> elseStatements = ast.getElseStatements();
        if (elseStatements.size() > 0) {
            writer.write("} else {");
            newline(0);
            for (Ast.Statement statement : elseStatements) {
                writer.write("    ");
                visit(statement);
                newline(0);
            }
        }
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        writer.write("switch (");
        visit(ast.getCondition());
        writer.write(") {");
        List<Ast.Statement.Case> caseList = ast.getCases();
        for (Ast.Statement.Case caseStatement : caseList) {
            newline(1);
            visit(caseStatement);
        }
        newline(0);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        List<Ast.Statement> statementList = ast.getStatements();
        Optional<Ast.Expression> optional = ast.getValue();
        // if it a case statement
        if (optional.isPresent()) {
            writer.write("case ");
            Ast.Expression expression = optional.get();
            visit(expression);
            writer.write(":");
            for (Ast.Statement statement : statementList) {
                newline(2);
                visit(statement);
            }
            newline(2);
            writer.write("break;");
        }
        // if it is a default statement
        else {
            writer.write("default:");
            for (Ast.Statement statement : statementList) {
                newline(2);
                visit(statement);
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");
        List<Ast.Statement> statementList = ast.getStatements();
        for (Ast.Statement statement : statementList) {
            newline(1);
            visit(statement);
            writer.write(";");
        }
        if (statementList.isEmpty()) {
            writer.write("}");
        }
        else {
            newline(0);
            writer.write("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Environment.Type type = ast.getType();
        if (type.equals(Environment.Type.CHARACTER)) {
            String str = "\'" + ast.getLiteral() + "\'";
            print(str);
        }
        else if (type.equals(Environment.Type.STRING)) {
            String str = "\"" + ast.getLiteral() + "\"";
            print(str);
        }
        else if (type.equals(Environment.Type.DECIMAL)) {
            Object obj = ast.getLiteral();
            String str = obj.toString();
            print(new BigDecimal(str));
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        if (operator.equals("^")) {
            writer.write("Math.pow(");
            visit(ast.getLeft());
            writer.write(",");
            visit(ast.getRight());
            writer.write(")");
        }
        else {
            visit(ast.getLeft());
            print(" " + operator + " ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        Optional<Ast.Expression> optional = ast.getOffset();
        if (optional.isPresent()) {
            Ast.Expression expression = optional.get();
            writer.write("[");
            visit(expression);
            writer.write("]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        writer.write("(");
        List<Ast.Expression> argumentList = ast.getArguments();
        for (int i = 0; i < argumentList.size(); i++) {
            if ((i + 1) != argumentList.size()) {
                writer.write(", ");
            }
            visit(argumentList.get(i));
        }
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> expressionList = ast.getValues();
        writer.write("{");
        for (int i = 0; i < expressionList.size() - 1; i++) {
            visit(expressionList.get(i));
            writer.write(", ");
        }
        visit(expressionList.get(expressionList.size() - 1));
        writer.write("}");
        return null;
    }
}
