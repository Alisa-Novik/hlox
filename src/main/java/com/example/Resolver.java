package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.example.Expr.Assign;
import com.example.Expr.Binary;
import com.example.Expr.Call;
import com.example.Expr.Grouping;
import com.example.Expr.Literal;
import com.example.Expr.Logical;
import com.example.Expr.Unary;
import com.example.Expr.Variable;
import com.example.Stmt.Block;
import com.example.Stmt.Expression;
import com.example.Stmt.Function;
import com.example.Stmt.If;
import com.example.Stmt.Print;
import com.example.Stmt.Return;
import com.example.Stmt.Var;
import com.example.Stmt.While;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private enum FunctionType {
        NONE, FUNCTION
    }

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            resolve(stmt);
        }
    }

    private void resolve(Expr expr) {

    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }
    private void endScope() {
        scopes.pop();
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Function stmt, FunctionType functionType) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = functionType;

        beginScope();
        for (Token param : stmt.params) {
            declare(param);
            define(param);
        }
        resolve(stmt.body);
        endScope();
        currentFunction = enclosingFunction; 
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void define(Token name) {
        if (scopes.empty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Variable with this name is already in the scope.");
        }
        scope.put(name.lexeme, false);
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitBinaryExpr(Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in it's own initializer");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
            }
        }
    }

    @Override
    public Void visitAssignExpr(Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }
}
