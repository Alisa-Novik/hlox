package com.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generateAst <output_dir>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
            "Binary : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal : Object value",
            "Unary : Token operator, Expr right"
        ));
    }

    private static void defineAst(String outputDir, String baseClass, List<String> types) throws IOException {
        String path = outputDir + "/" + baseClass + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);
        
        writer.println("package com.example;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseClass + " {");

        defineVisitor(writer, baseClass, types);

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseClass, className, fields);
        }
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseClass, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseClass + "(" + typeName + " " + baseClass.toLowerCase() + ");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseClass, String className, String fieldList) {
        writer.println();
        writer.println("    static class " + className + " extends " + baseClass + " {");

        writer.println("        " + className + " (" + fieldList + ") {");

        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");
        writer.println();
        
        writer.println("        @Override");
        writer.println("        <R> R accept (Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseClass + "(this);");
        writer.println("        }");
        writer.println();

        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println("    }");
    } 
}
