package com.example;

import java.util.List;

import com.example.Stmt.Function;

class LoxFunction implements LoxCallable {

    private final Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
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
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }
    
}

