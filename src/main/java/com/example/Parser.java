package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.Expr.Get;
import com.example.Expr.Variable;
import com.example.Stmt.Block;
import com.example.Stmt.If;
import com.example.Stmt.While;
import com.example.Token.TokenType;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    } 

    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDeclaration();
            if (match(TokenType.FUN)) return function("function");
            if (match(TokenType.VAR)) return varStatement();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Extract superclass name.");
            superclass = new Expr.Variable(previous());
        }
        consume(TokenType.LEFT_BRACE, "Expect '{' after class declaration");
        List<Stmt.Function> methods = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class declaration");
        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if(!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() > 255) {
                    error(peek(), "Too many arguments for a function (>255)");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name"));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect { before " + kind + " body");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt statement() {
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.LEFT_BRACE)) return new Block(block());
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varStatement();
        } else {
            initializer = expressionStatement();
        }
        
        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression(); 
        }
        consume(TokenType.SEMICOLON, "Expect ';' after 'condition'.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'condition'.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'while'.");
        return new While(condition, statement());
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'."); 
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if'."); 

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new If(condition, thenBranch, elseBranch);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect } after block.");
        return statements;
    }

    private Stmt varStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expression);
    }

    private Stmt printStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
        
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Variable) {
                Token name = ((Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        
        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Error error(Token token, String message) {
        Lox.error(token.line, message);
        return new Error();
    }

    private Token consume(TokenType type, String errorMsg) {
        if(check(type)) return advance();
        throw error(peek(), errorMsg);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ) after expression");
            return new Expr.Grouping(expr);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }
        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super.'");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name");
            return new Expr.Super(keyword, method);
        }

        throw error(peek(), "Expect expression");
    }

    private void synchronize() {
        advance();
        
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) {
                return;
            }
            
            switch (peek().type) {
                case CLASS:
                case COMMA:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 args.");
                }
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr factor() {
        Expr expr = unary();

        while(match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while(match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while(match(TokenType.GREATER_EQUAL, TokenType.GREATER, TokenType.LESS_EQUAL, TokenType.LESS)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType toCheck) {
        if (isAtEnd()) return false;
        return peek().type == toCheck;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }
}
