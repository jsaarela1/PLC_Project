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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        Environment.Function function = ast.getFunction();
        print(function.getReturnType());
        writer.write(" ");
        print(function.getJvmName());
        List<String> parameterList = ast.getParameters();



        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        Ast.Expression expression = ast.getExpression();
        visit(expression);
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
            writer.write(";");
            newline(0);
        }
        List<Ast.Statement> elseStatements = ast.getElseStatements();
        if (elseStatements.size() > 0) {
            writer.write("} else {");
            newline(0);
            for (Ast.Statement statement : elseStatements) {
                writer.write("    ");
                visit(statement);
                writer.write(";");
                newline(0);
            }
        }
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");
        newline(0);
        List<Ast.Statement> statementList = ast.getStatements();
        for (Ast.Statement statement : statementList) {
            writer.write("    ");
            visit(statement);
            writer.write(";");
            newline(0);
        }
        writer.write("}");
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
        throw new UnsupportedOperationException(); //TODO
    }
}
