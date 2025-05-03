import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java_cup.runtime.*;
import absyn.*;

public class CM {
    public static void main(String[] args) {
        boolean displayAST = false;
        boolean semanticAnalysis = false;
        boolean codeGeneration = false;
        String inputFile = null;
        
        // Process command-line arguments
        for (String arg : args) {
            if ("-a".equals(arg)) {
                displayAST = true;
            } else if ("-s".equals(arg)) {
                semanticAnalysis = true;
            } else if ("-c".equals(arg)) {
                codeGeneration = true;
            } else {
                inputFile = arg;
            }
        }
        
        if (inputFile == null) {
            System.err.println("Error: No input file provided.");
            return;
        }
        
        try (Reader reader = new FileReader(inputFile)) {
            parseAndProcessAST(reader, displayAST, semanticAnalysis, codeGeneration, inputFile);
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + inputFile);
        } catch (IOException e) {
            System.err.println("Error: I/O Exception occurred.");
        } catch (Exception e) {
            System.err.println("Error: Parsing failed.");
            e.printStackTrace();
        }
    }
    
    private static void parseAndProcessAST(Reader reader, boolean displayAST, boolean semanticAnalysis, 
                                          boolean codeGeneration, String inputFile) throws Exception {
        // Initialize lexer and parser
        Lexer lexer = new Lexer(reader);
        parser p = new parser(lexer);
        
        // Parse input and build the AST
        Symbol parseResult = p.parse();
        Absyn syntaxTree = (Absyn) parseResult.value;
        
        if (syntaxTree == null) {
            System.err.println("Error: Parsing resulted in a null AST.");
            return;
        }
        
        // Extract filename without extension
        Path path = Paths.get(inputFile);
        Path file = path.getFileName();
        String filename = file.toString();
        filename = filename.substring(0, filename.lastIndexOf("."));
        
        if (displayAST) {
            try (PrintStream out = new PrintStream(new FileOutputStream("test/" + filename + ".abs"))) {
                System.setOut(out);
                System.out.println("The abstract syntax tree is:");
                ShowTreeVisitor visitor = new ShowTreeVisitor();
                syntaxTree.accept(visitor, 0);
            }
        }
        
        SemanticAnalyzer semanticVisitor = null;
        
        if (semanticAnalysis || codeGeneration) {
            semanticVisitor = new SemanticAnalyzer();
            syntaxTree.accept(semanticVisitor, 0);
            
            if (semanticAnalysis) {
                try (PrintStream out = new PrintStream(new FileOutputStream("test/" + filename + ".sym"))) {
                    System.setOut(out);
                    System.out.println("Entering the global scope:");
                    semanticVisitor.printSymbolTable(1);
                    System.out.println("Leaving the global scope");
                }
            }
        }
        
        // Code generation
        if (codeGeneration) {
            if (semanticVisitor != null && !semanticVisitor.hasErrors()) {
                String tmFile = "test/" + filename + ".tm";
                // Pass the AST (cast to DecList) to the CodeGenerator constructor.
                new CodeGenerator(tmFile, (DecList) syntaxTree);
                System.out.println("Code generation successful. Output written to " + tmFile);
            } else {
                System.err.println("Semantic errors found. Code generation aborted.");
            }
        }
        
        // Reset standard output
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }
}
