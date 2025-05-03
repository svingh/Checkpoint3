import absyn.*;
import java.io.*;
import java.util.ArrayList;

public class CodeGenerator {

    // TM simulator registers
    public static final int AC = 0;      // Accumulator
    public static final int AC1 = 1;     // Additional accumulator register
    public static final int FP = 5;      // Frame Pointer
    public static final int GP = 6;      // Global Pointer
    public static final int PC = 7;      // Program Counter
    // Code generation pointers
    private int emitLoc = 0;     // current instruction location
    private int highEmitLoc = 0; // next available location
    private int globalOffset = 0; // offset for global variables

    // Output file for generated code
    private String fName;

    // The AST of the program (a DecList)
    private DecList program;

    // Symbol table using your provided classes.
    private SymbolTable symTable;

    // --- Minimal scope management (no-ops) ---
    private void newScope() {
        // No nested scopes implemented.
    }
    
    private void removeScope() {
        // No nested scopes implemented.
    }
    
    // Helper to add a symbol.
    private void addSymbol(String name, SymbolInfo sym) {
        symTable.insert(name, sym);
    }
    
    // Helper to look up a symbol.
    private SymbolInfo getSymbol(String name) {
        return symTable.lookup(name);
    }
    
    // Returns 0 if symbol is global, nonzero otherwise.
    private int symExists(String name) {
        SymbolInfo sym = getSymbol(name);
        return (sym != null && sym.isGlobal()) ? 0 : 1;
    }

    // Constructor: pass output file name and the AST (DecList).
    public CodeGenerator(String fName, DecList program) {
        this.fName = fName;
        this.program = program;
        this.symTable = new SymbolTable();
        // Begin code generation.
        visit(program);
    }

    // --- Code emission routines ---
    public int emitSkip(int dist) {
        int i = emitLoc;
        emitLoc += dist;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
        return i;
    }
    
    public void emitBackup(int loc) {
        if (loc > highEmitLoc) {
            emitComment("BUG in emitBackup: loc > highEmitLoc");
        }
        emitLoc = loc;
    }
    
    public void emitRestore() {
        emitLoc = highEmitLoc;
    }
    
    public void emitComment(String comment) {
        // Comments starting with '*' are printed on a separate line.
        writeCode("* " + comment + "\n");
    }
    
    public void emitRM(String op, int r, int offset, int r1, String comment) {
        String s = emitLoc + ": " + op + " " + r + "," + offset + "(" + r1 + ")";
        writeCode(s + " " + comment + "\n");
        emitLoc++;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }
    
    // Modified to output comment on same line.
    public void emitRMAbs(String op, int r, int a, String comment) {
        String s = emitLoc + ": " + op + " " + r + "," + (a - (emitLoc + 1)) + "(" + PC + ")";
        writeCode(s + " " + comment + "\n");
        emitLoc++;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }
    
    // Modified to output comment on same line.
    public void emitOp(String op, int dest, int r, int r1, String comment) {
        String s = emitLoc + ": " + op + " " + dest + "," + r + "," + r1;
        writeCode(s + " " + comment + "\n");
        emitLoc++;
    }
    
    public void writeCode(String s) {
        try (PrintWriter output = new PrintWriter(new FileOutputStream(this.fName, true))) {
            output.print(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    // --- Visitor methods for AST nodes ---
    public void visit(DecList d) {
        // Clear output file.
        try (PrintWriter output = new PrintWriter(this.fName)) {
            // Cleared.
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        newScope();
        
        // Insert predefined functions "input" and "output" as built-ins.
        FunctionSymbolInfo inputSym = new FunctionSymbolInfo(0, true);
        addSymbol("input", inputSym);
        FunctionSymbolInfo outputSym = new FunctionSymbolInfo(0, true);
        addSymbol("output", outputSym);
        
        // Emit prelude.
        emitComment("Prelude");
        // Use LDC to load a constant value into GP so that clearing memory location 0 doesn't affect it.
        emitRM("LDC", GP, 55, 0, "Load GP with max address");
        emitRM("LDA", FP, 0, GP, "Copy GP to FP");
        emitRM("ST", 0, 0, 0, "Clear location 0");
        int jumpLoc = emitSkip(1);  // Reserve jump space for I/O routines
        
        // Input routine.
        emitComment("Input routine");
        emitRM("ST", 0, -1, FP, "Store return address");
        emitOp("IN", 0, 0, 0, "Input");
        emitRM("LD", PC, -1, FP, "Return from input");
        
        // Output routine.
        emitComment("Output routine");
        emitRM("ST", 0, -1, FP, "Store return address");
        emitRM("LD", 0, -2, FP, "Load output value");
        emitOp("OUT", 0, 0, 0, "Output");
        emitRM("LD", PC, -1, FP, "Return from output");
        
        int ioJumpLoc = emitSkip(0);
        emitBackup(jumpLoc);
        emitRMAbs("LDA", PC, ioJumpLoc, "Jump around I/O routines");
        emitRestore();
        emitComment("End of prelude");
        
        // Process each top-level declaration.
        while (d != null) {
            if (d.head != null) {
                visit(d.head);
            }
            d = d.tail;
        }
        
        // Finale: jump to main.
        SymbolInfo mainSym = getSymbol("main");
        if (mainSym == null) {
            emitComment("Error: main function not found");
        } else {
            emitComment("Finale");
            emitRM("ST", FP, globalOffset, FP, "Push old frame pointer");
            emitRM("LDA", FP, globalOffset, FP, "Establish new frame pointer");
            emitRM("LDA", 0, 1, PC, "Load return pointer");
            emitRMAbs("LDA", PC, mainSym.getOffset(), "Jump to main function");
            emitRM("LD", FP, 0, FP, "Pop frame pointer");
            emitOp("HALT", 0, 0, 0, "Halt");
        }
        
        removeScope();
    }
    
    public void visit(Dec d) {
        if (d instanceof FunctionDec) {
            // Skip code generation for built-in functions.
            FunctionDec fd = (FunctionDec)d;
            if ("input".equals(fd.func) || "output".equals(fd.func))
                return;
            visit(fd);
        } else if (d instanceof VarDec) {
            globalOffset = visit((VarDec)d, globalOffset, false); // false because it's NOT a parameter
        }
    }
    
    public void visit(FunctionDec d) {
        // For built-in functions, do nothing.
        if ("input".equals(d.func) || "output".equals(d.func))
            return;
            
        emitComment("-> FunctionDec: " + d.func);
        int funcJumpLoc = emitSkip(1);  // Reserve jump location
        
        // Record function address.
        FunctionSymbolInfo funcSym = new FunctionSymbolInfo(emitLoc, true);
        addSymbol(d.func, funcSym);
        newScope();
        
        // Save return address.
        emitRM("ST", 0, -1, FP, "Store return address");
        
        // Process parameters.
        int offset = -2;
        offset = visit(d.params, offset, true);
        
        // Process function body.
        offset = visit(d.body, offset, false);
        
        // Return.
        emitRM("LD", PC, -1, FP, "Return to caller");
        
        int funcEndLoc = emitSkip(0);
        emitBackup(funcJumpLoc);
        emitRMAbs("LDA", PC, funcEndLoc, "Jump around function body");
        emitRestore();
        
        emitComment("<- FunctionDec: " + d.func);
        removeScope();
    }
    
    public int visit(VarDec d, int offset, boolean isParam) {
        if (d instanceof ArrayDec) {
            ArrayDec a = (ArrayDec)d;
            if (isParam) {
                ArraySymbolInfo arrSym = new ArraySymbolInfo(offset, 1, false);
                addSymbol(a.name, arrSym);
                return offset - 1;
            } else {
                if (offset == globalOffset) { // Global variable
                    ArraySymbolInfo arrSym = new ArraySymbolInfo(globalOffset, a.size, true);
                    addSymbol(a.name, arrSym);
                    emitComment("Allocating global array: " + a.name);
                    globalOffset += a.size;
                    return offset;
                } else { // Local
                    offset -= a.size;
                    ArraySymbolInfo arrSym = new ArraySymbolInfo(offset + 1, a.size, false);
                    addSymbol(a.name, arrSym);
                    emitComment("Allocating local array: " + a.name);
                    return offset;
                }
            }
        } else if (d instanceof SimpleDec) {
            SimpleDec s = (SimpleDec)d;
            SymbolInfo varSym;
            if (offset == globalOffset) {
                varSym = new SymbolInfo(globalOffset, true);
                addSymbol(s.identifier, varSym);
                emitComment("Allocating global variable: " + s.identifier);
                globalOffset++;
                return offset;
            } else {
                varSym = new SymbolInfo(offset, false);
                addSymbol(s.identifier, varSym);
                emitComment("Allocating local variable: " + s.identifier);
                return offset - 1;
            }
        }
        return offset;
    }
    
    public int visit(VarDecList d, int offset, boolean isParam) {
        while(d != null) {
            if (d.head != null) {
                offset = visit(d.head, offset, isParam);
            }
            d = d.tail;
        }
        return offset;
    }
    
    public int visit(Exp e, int offset, boolean isAddress) {
        if (e instanceof AssignExp) {
            visit((AssignExp)e, offset);
        } else if (e instanceof IfExp) {
            visit((IfExp)e, offset);
        } else if (e instanceof WhileExp) {
            visit((WhileExp)e, offset);
        } else if (e instanceof CompoundExp) {
            offset = visit((CompoundExp)e, offset);
        } else if (e instanceof OpExp) {
            visit((OpExp)e, offset);
        } else if (e instanceof CallExp) {
            visit((CallExp)e, offset);
        } else if (e instanceof IntExp) {
            visit((IntExp)e);
        } else if (e instanceof VarExp) {
            visit((VarExp)e, offset, isAddress);
        } else if (e instanceof BoolExp) {
            visit((BoolExp)e);
        } else if (e instanceof NilExp) {
            // Handle NilExp if needed.
        }else if (e instanceof ReturnExp) {
            visit((ReturnExp)e, offset);
        }
        return offset;
    }
    
    public void visit(ExpList list, int offset) {
        while(list != null) {
            if (list.head != null) {
                visit(list.head, offset, false);
            }
            list = list.tail;
        }
    }
    
    // Revised assignment: store the right-hand value in the left-hand variable’s allocated offset.
    public void visit(AssignExp e, int offset) {
        emitComment("-> AssignExp");
        // Evaluate LHS to get its address (for simple variables or array element)
        if (e.lhs.variable instanceof SimpleVar) {
            visit((SimpleVar)e.lhs.variable, offset, true);
        } else if (e.lhs.variable instanceof IndexVar) {
            visit((IndexVar)e.lhs.variable, offset, true);
        }
        // Evaluate RHS to get the value in AC.
        visit(e.rhs, offset, false);
        // Retrieve the symbol information from the variable name.
        SymbolInfo varSym = null;
        if (e.lhs.variable instanceof SimpleVar)
            varSym = getSymbol(((SimpleVar)e.lhs.variable).name);
        else if (e.lhs.variable instanceof IndexVar)
            varSym = getSymbol(((IndexVar)e.lhs.variable).name);
        if (varSym != null) {
            if (varSym.isGlobal()) {
                emitRM("ST", AC, varSym.getOffset(), GP, "Store assignment result");
            } else {
                emitRM("ST", AC, varSym.getOffset(), FP, "Store assignment result");
            }
        } else {
            // Fallback if symbol info is missing.
            emitRM("ST", AC, 0, FP, "Store assignment result");
        }
        emitComment("<- AssignExp");
    }
    
    public void visit(IfExp e, int offset) {
        visit(e.test, offset, false);    // test => AC=1 if TRUE, 0 if FALSE

        // The next line (elseJumpLoc) is the spot we do "JEQ AC, elseStartLoc"
        int elseJumpLoc = emitSkip(1);

        // THEN part
        if (e.then != null) {
            visit(e.then, offset, false);
        }

        // Reserve an unconditional jump for skipping the else
        int afterElseJumpLoc = emitSkip(1);

        int elseStartLoc = emitLoc;   // else block starts here
        if (e.elsee != null) {
            visit(e.elsee, offset, false);
        }
        int afterElseLoc = emitLoc;   // end of if statement

        // Patch the "JEQ AC, elseStartLoc"
        emitBackup(elseJumpLoc);
        emitRMAbs("JEQ", AC, elseStartLoc, "If AC==0, jump to else");
        emitRestore();

        // Patch the unconditional jump that skips the else
        emitBackup(afterElseJumpLoc);
        emitRMAbs("LDA", PC, afterElseLoc, "Skip else part");
        emitRestore();
    }


    public void visit(WhileExp e, int offset) {
        emitComment("-> WhileExp");
        newScope();
        int testLoc = emitSkip(0);
        visit(e.test, offset, false);
        int jumpLoc = emitSkip(1);
        visit(e.body, offset, false);
        emitRMAbs("LDA", PC, testLoc, "Jump back to test");
        int exitLoc = emitSkip(0);
        emitBackup(jumpLoc);
        emitRMAbs("JEQ", AC, exitLoc, "Exit while loop");
        emitRestore();
        removeScope();
        emitComment("<- WhileExp");
    }
    
    public int visit(CompoundExp e, int offset) {
        emitComment("-> CompoundExp");
        offset = visit(e.decs, offset, false);
        visit(e.exps, offset);
        emitComment("<- CompoundExp");
        return offset;
    }
    
    public void visit(OpExp e, int offset) {
        emitComment("-> OpExp");
        // Evaluate left operand and push its value into AC1.
        visit(e.left, offset, false);
        emitRM("ST", AC, offset, FP, "Push left operand");
        offset--;
        // Evaluate right operand into AC.
        visit(e.right, offset, false);
        // Retrieve the left operand into AC1.
        emitRM("LD", AC1, offset + 1, FP, "Load left operand");
        switch (e.op) {
            case OpExp.PLUS:
                emitOp("ADD", AC, AC1, AC, "Add");
                break;
                
            case OpExp.MINUS:
                emitOp("SUB", AC, AC1, AC, "Subtract");
                break;
                
            case OpExp.TIMES:
                emitOp("MUL", AC, AC1, AC, "Multiply");
                break;
                
            case OpExp.OVER:
                emitOp("DIV", AC, AC1, AC, "Divide");
                break;
                
            case OpExp.EQ:
            // AC ← (AC1 - AC)
                emitOp("SUB", AC, AC1, AC, "Subtract for EQ");
                emitRM("JEQ", AC, 2, PC, "Jump if equal");
                emitRM("LDC", AC, 0, 0, "False case");
                emitRM("LDA", PC, 1, PC, "Unconditional jump");
                emitRM("LDC", AC, 1, 0, "True case");
                break;

            case OpExp.NE:
                emitOp("SUB", AC, AC1, AC, "Subtract for NE");
                emitRM("JNE", AC, 2, PC, "Jump if not equal");
                emitRM("LDC", AC, 0, 0, "False case");
                emitRM("LDA", PC, 1, PC, "Unconditional jump");
                emitRM("LDC", AC, 1, 0, "True case");
                break;

            case OpExp.LT:
                emitOp("SUB", AC, AC1, AC, "Subtract for LT");
                emitRM("JLT", AC, 2, PC, "Jump if less than");
                emitRM("LDC", AC, 0, 0, "False case");
                emitRM("LDA", PC, 1, PC, "Unconditional jump");
                emitRM("LDC", AC, 1, 0, "True case");
                break;

            case OpExp.GT:
                emitOp("SUB", AC, AC1, AC, "Subtract for GT");
                emitRM("JGT", AC, 2, PC, "Jump if greater than");
                emitRM("LDC", AC, 0, 0, "False case");
                emitRM("LDA", PC, 1, PC, "Unconditional jump");
                emitRM("LDC", AC, 1, 0, "True case");
                break;

            default:
                emitComment("Unrecognized operator");
        }
        emitComment("<- OpExp");
    }
    
    public void visit(CallExp e, int offset) {
        String funcName = e.func;
        // Special handling for built-in functions:
        if ("input".equals(funcName)) {
            // For input, simply emit the IN instruction.
            emitOp("IN", AC, 0, 0, "Built-in input");
            return;
        } else if ("output".equals(funcName)) {
            // For output, first evaluate its argument.
            if (e.args != null) {
                visit(e.args.head, offset, false);
            }
            emitOp("OUT", AC, 0, 0, "Built-in output");
            return;
        }
        // Otherwise, normal user-defined call:
        emitComment("-> CallExp: " + funcName);
        FunctionSymbolInfo funcSym = (FunctionSymbolInfo) getSymbol(funcName);
        int argOffset = offset - 2;
        ExpList args = e.args;
        while (args != null) {
            if (args.head != null) {
                visit(args.head, argOffset, false);
                emitRM("ST", AC, argOffset, FP, "Push argument");
                argOffset--;
            }
            args = args.tail;
        }
        emitRM("ST", FP, offset, FP, "Push old FP");
        emitRM("LDA", FP, offset, FP, "Set new FP");
        emitRM("LDA", AC, 1, PC, "Load return pointer");
        emitRMAbs("LDA", PC, funcSym.getOffset(), "Jump to function: " + funcName);
        emitRM("LD", FP, 0, FP, "Restore FP");
        emitComment("<- CallExp");
    }
    
    public void visit(IntExp e) {
        emitComment("-> IntExp");
        emitRM("LDC", AC, e.value, 0, "Load constant " + e.value);
        emitComment("<- IntExp");
    }
    
    public void visit(BoolExp e) {
        emitComment("-> BoolExp");
        emitRM("LDC", AC, e.value ? 1 : 0, 0, "Load boolean constant");
        emitComment("<- BoolExp");
    }
    
    public void visit(VarExp e, int offset, boolean isAddress) {
        if (e.variable instanceof SimpleVar) {
            visit((SimpleVar)e.variable, offset, isAddress);
        } else if (e.variable instanceof IndexVar) {
            visit((IndexVar)e.variable, offset, isAddress);
        }
    }
    
    public void visit(SimpleVar v, int offset, boolean isAddress) {
        emitComment("-> SimpleVar: " + v.name);
        SymbolInfo varSym = getSymbol(v.name);
        if (symExists(v.name) == 0) {
            if (isAddress) {
                emitRM("LDA", AC, varSym.getOffset(), GP, "Load address of " + v.name);
            } else {
                emitRM("LD", AC, varSym.getOffset(), GP, "Load value of " + v.name);
            }
        } else {
            if (isAddress) {
                emitRM("LDA", AC, varSym.getOffset(), FP, "Load address of " + v.name);
            } else {
                emitRM("LD", AC, varSym.getOffset(), FP, "Load value of " + v.name);
            }
        }
        emitComment("<- SimpleVar");
    }
    
    public void visit(IndexVar e, int offset, boolean isAddress) {
        emitComment("-> IndexVar: " + e.name);

        SymbolInfo sym = getSymbol(e.name);
        if (!(sym instanceof ArraySymbolInfo)) {
            emitComment("Error: " + e.name + " is not declared as an array.");
            return;
        }

        ArraySymbolInfo arrSym = (ArraySymbolInfo) sym;

        // Step 1: Evaluate index expression and store in AC1
        visit(e.index, offset, false); // result in AC
        emitRM("ST", AC, offset, FP, "Store index temporarily");
        
        // Step 2: Load base address of array into AC
        if (arrSym.isGlobal()) {
            emitRM("LDA", AC, arrSym.getOffset(), GP, "Load base address of global array " + e.name);
        } else {
            emitRM("LDA", AC, arrSym.getOffset(), FP, "Load base address of local array " + e.name);
        }

        // Step 3: Retrieve index back into AC1
        emitRM("LD", AC1, offset, FP, "Reload index value into AC1");

        // Step 4: Add index to base to get effective address
        emitOp("ADD", AC, AC, AC1, "Compute effective address: base + index");

        // Step 5: If we're accessing the value (not just the address), dereference it
        if (!isAddress) {
            emitRM("LD", AC, 0, AC, "Load value at computed array index");
        }

        emitComment("<- IndexVar");
    }

    
    public void visit(ReturnExp e, int offset) {
        emitComment("-> ReturnExp");
        visit(e.returnValue, offset, false);
        emitRM("LD", PC, -1, FP, "Return to caller");
        emitComment("<- ReturnExp");
    }
}
