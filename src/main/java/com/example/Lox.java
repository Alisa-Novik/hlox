package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.example.Token.TokenType;

public class Lox 
{
    private static Interpreter interpreter = new Interpreter();

    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

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
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        System.out.println(tokens);
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) return;
 
        // System.out.println((new AstPrinter()).print(expr));
        interpreter.interpret(statements);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at the end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(String.format("[line %d] Error %s: %s", line, where, message));
        hadError = true;
    }

    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
