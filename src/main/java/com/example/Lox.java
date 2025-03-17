package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Lox 
{
    private static boolean hadError;

    public static void main( String[] args ) throws IOException
    {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader in = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(in);

        while (true) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] program = Files.readAllBytes(Path.of(path));
        run(new String(program, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
    }

    private static void run(String source) {
        Scanner sc = new Scanner(source);
        List<String> tokens = sc.tokens().toList();

        for (String token : tokens) {
            System.out.println("Token: " + token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(String.format("[line %d] Error %s: %s", line, where, message));
        hadError = true;
    }
}
