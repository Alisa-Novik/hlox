package com.example;

import java.util.List;

import com.example.Stmt.Function;

class LoxClass {

    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    public String toString() {
        return "<class " + name + ">";
    }
}

