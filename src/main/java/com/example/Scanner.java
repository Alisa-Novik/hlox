package com.example;

import java.util.ArrayList;
import java.util.List;

import com.example.App.Token;
import com.example.App.TokenType;

public class Scanner 
{
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd) {
            start = current;
            scanTokens();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
    }
}
