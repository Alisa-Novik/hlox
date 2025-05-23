package com.example;

import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LoxInstanceTest extends TestCase {
    public LoxInstanceTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(LoxInstanceTest.class);
    }

    public void testFieldAccess() {
        LoxClass klass = new LoxClass("Foo", null, new HashMap<>());
        LoxInstance instance = new LoxInstance(klass);
        Token token = new Token(Token.TokenType.IDENTIFIER, "bar", null, 1);
        instance.set(token, 42);
        assertEquals(42, instance.get(token));
    }
}
