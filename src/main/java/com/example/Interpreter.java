package com.example;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.Expr.Assign;
import com.example.Expr.Binary;
import com.example.Expr.Call;
import com.example.Expr.Get;
import com.example.Expr.Grouping;
import com.example.Expr.Literal;
import com.example.Expr.Logical;
import com.example.Expr.Set;
import com.example.Expr.This;
import com.example.Expr.Unary;
import com.example.Expr.Variable;
import com.example.Stmt.Block;
import com.example.Stmt.Class;
import com.example.Stmt.Expression;
import com.example.Stmt.Function;
import com.example.Stmt.If;
import com.example.Stmt.Print;
import com.example.Stmt.Var;
import com.example.Stmt.While;
import com.example.Token.TokenType;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(final Interpreter interpreter, final List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(final List<Stmt> statements) {
        try {
            for (final Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (final RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(final Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(final Object value) {
        if (value == null) {
            return "nil";
        }

        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return value.toString(); 
    }

    @Override
    public Object visitBinaryExpr(final Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                return (double)left / (double)right;
            case STAR:
                return (double)left * (double)right;
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return !isEqual(left, right);
            case BANG:
                checkNumberOperand(expr.operator, left, right);
                return isEqual(left, right);
        }
         
        // Unreacheable
        return null;
    }

    @Override
    public Object visitGroupingExpr(final Grouping expr) {
        return evaluate(expr.expression);
    }


    @Override
    public Object visitLiteralExpr(final Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(final Unary expr) {
        final Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS: 
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreacheable
        return null;
    }

    private void checkNumberOperand(final Token operator, final Object left, final Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }
    
    private void checkNumberOperand(final Token operator, final Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private boolean isEqual(final Object a, final Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean isTruthy(final Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;

        return true;
    }

    private Object evaluate(final Expr expression) {
        return expression.accept(this);
    }

    @Override
    public Void visitExpressionStmt(final Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(final Print stmt) {
        final Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(final Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(final Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(final Token name, final Expr expr) {
        final Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(final Assign expr) {
        final Object value = evaluate(expr.value);
        final Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Void visitBlockStmt(final Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(final List<Stmt> statements, final Environment environment) {
        final Environment prev = this.environment;
        try {
            this.environment = environment;
            for (final Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            this.environment = prev;
        }
    }

    @Override
    public Void visitIfStmt(final If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Object visitLogicalExpr(final Logical expr) {
        final Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitWhileStmt(final While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(final Call expr) {
        final Object callee = evaluate(expr.callee);
        final List<Object> arguments = new ArrayList<>();
        for (final Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }
        final LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size());
        }
        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(final Function stmt) {
        final LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(final Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }

    void resolve(final Expr expr, final int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitClassStmt(final Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if(!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            }
        }
        environment.define(stmt.name.lexeme, null);
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(
                method,
                environment,
                method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        LoxClass kclass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);
        environment.assign(stmt.name, kclass);
        return null;
    }

    @Override
    public Object visitGetExpr(final Get expr) {
        final Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties");
    }

    @Override
    public Object visitSetExpr(Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }
        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(This expr) {
        return lookupVariable(expr.keyword, expr);
    }
}

