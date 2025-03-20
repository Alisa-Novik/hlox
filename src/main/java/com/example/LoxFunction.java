package com.example;

import java.util.List;

import com.example.Stmt.Function;

class LoxFunction implements LoxCallable {

    private final Function declaration;
    private final Environment closure;

    LoxFunction(Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }
    
    @Override
    public int arity() {
        return declaration.params.size();
    }

    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue;
        }
        return null;
    }
    
}

