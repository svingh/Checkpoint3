import absyn.*;
import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;

public class SemanticAnalyzer implements AbsynVisitor {

    // *** FIX (1) Hash tables: storing each scope's symbols in a HashMap ***
    public HashMap<String, ArrayList<NodeType>> symbolTable;
    public Stack<String> scopeStack;
    public int nestLevel = 0;

    // Type codes: 0=bool, 1=int, 2=void
    public static final String[] TYPE_NAMES = {"bool", "int", "void"};
    final static int INDENT_SPACES = 4;

    private boolean hasErrors = false;

    public SemanticAnalyzer() {
        symbolTable = new HashMap<>();
        scopeStack = new Stack<>();
        scopeStack.add("global");
        initPredefinedFunctions();
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    private void initPredefinedFunctions() {
        // Create a prototype for input(): int input(void)
        FunctionDec inputFunc = new FunctionDec(0, 0, new NameTy(0, 0, 1), "input", null, new NilExp(0, 0));
        NodeType inputNode = new NodeType("input", inputFunc, "global");
        insert("global", inputNode);

        // Create a prototype for output(int): void output(int)
        SimpleDec param = new SimpleDec(0, 0, new NameTy(0, 0, 1), "x");
        VarDecList paramList = new VarDecList(param, null);
        FunctionDec outputFunc = new FunctionDec(0, 0, new NameTy(0, 0, 2), "output", paramList, new NilExp(0, 0));
        NodeType outputNode = new NodeType("output", outputFunc, "global");
        insert("global", outputNode);
    }


    private void indent(int level) {
        for (int i = 0; i < level * INDENT_SPACES; i++) {
            System.out.print(" ");
        }
    }

    private void reportError(String message) {
        System.err.println(message);
        hasErrors = true;
    }

    public void insert(String scope, NodeType node) {
        ArrayList<NodeType> list = symbolTable.get(scope);
        if (list == null) {
            list = new ArrayList<>();
            symbolTable.put(scope, list);
        }
        list.add(node);
    }

    public NodeType nodeExists(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            String scope = scopeStack.get(i);
            ArrayList<NodeType> list = symbolTable.get(scope);
            if (list != null) {
                for (NodeType node : list) {
                    if (node.name.equals(name)) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    public NodeType functionExists(String name) {
        ArrayList<NodeType> list = symbolTable.get("global");
        if (list != null) {
            for (NodeType node : list) {
                if (node.name.equals(name)) {
                    return node;
                }
            }
        }
        return null;
    }

    public int evaluateExpression(Exp exp) {
        if (exp == null) return -1;
        if (exp instanceof OpExp) {
            OpExp op = (OpExp) exp;
            int leftType = evaluateExpression(op.left);
            int rightType = evaluateExpression(op.right);

            if (leftType == -1 || rightType == -1) {
                return -1;
            }

            // *** FIX: Additional check for boolean operators (issues with Boolean expressions) ***
            if (op.op == OpExp.AND || op.op == OpExp.OR) {
                // both sides must be bool (0)
                if (leftType != 0 || rightType != 0) {
                    return -1;
                }
                return 0; // The result of a boolean op is bool
            } else if (op.op == OpExp.NOT) {
                return -1;
            } else {
                if (leftType == rightType) {
                    return leftType; 
                }
                return -1;
            }
        } else if (exp instanceof CallExp) {
            CallExp call = (CallExp) exp;
            NodeType n = functionExists(call.func);
            if (n != null && n.def instanceof FunctionDec) {
                return ((FunctionDec) n.def).result.typ;
            } else {
                return -1; 
            }
        } else if (exp instanceof VarExp) {
            return varType((VarExp) exp);
        } else {
            return exp.getType();
        }
    }

    public int varType(VarExp exp) {
        if (exp == null || exp.variable == null) return -1;
        String name;
        if (exp.variable instanceof SimpleVar) {
            name = ((SimpleVar) exp.variable).name;
        } else if (exp.variable instanceof IndexVar) {
            name = ((IndexVar) exp.variable).name;
        } else {
            return -1;
        }
        NodeType node = nodeExists(name);
        if (node == null) return -1;  // undefined variable

        // If it's a simple variable
        if (node.def instanceof SimpleDec) {
            return ((SimpleDec) node.def).typeSpecifier.typ;
        } 
        // If it's an array
        else if (node.def instanceof ArrayDec) {
            // referencing the element => same type as array's base type
            return ((ArrayDec) node.def).typ.typ;
        }
        return -1;
    }

    private String getTypeName(int typeCode) {
        if (typeCode >= 0 && typeCode < TYPE_NAMES.length) {
            return TYPE_NAMES[typeCode];
        }
        return "unknown";
    }

    private String getParamTypes(VarDecList params) {
        if (params == null) return "void";
        StringBuilder sb = new StringBuilder();
        VarDecList cur = params;
        while (cur != null) {
            if (cur.head instanceof SimpleDec) {
                SimpleDec sdec = (SimpleDec) cur.head;
                sb.append(getTypeName(sdec.typeSpecifier.typ));
            } else if (cur.head instanceof ArrayDec) {
                ArrayDec adec = (ArrayDec) cur.head;
                sb.append(getTypeName(adec.typ.typ)).append("[]");
            }
            cur = cur.tail;
            if (cur != null) sb.append(", ");
        }
        return sb.toString();
    }

    // ------------------- Visitor Methods -------------------

    @Override
    public void visit(VarExp exp, int level) {
        if (exp.variable instanceof SimpleVar) {
            // *** FIX for "Didn't capture undefined": check if var is declared
            String name = ((SimpleVar) exp.variable).name;
            NodeType node = nodeExists(name);
            if (node == null) {
                reportError("Error: Variable " + name + " is not declared at line " + (exp.row + 1));
            }
        } else if (exp.variable instanceof IndexVar) {
            IndexVar iv = (IndexVar) exp.variable;
            NodeType node = nodeExists(iv.name);
            if (node == null) {
                reportError("Error: Array " + iv.name + " is not declared at line " + (exp.row + 1));
            } else {
                // *** FIX for "array range/index are int"
                int idxType = evaluateExpression(iv.index);
                if (idxType != 1) {
                    reportError("Error in line " + (iv.row + 1) + ": Array index must be int");
                }
            }
        }
    }

    @Override
    public void visit(SimpleVar var, int level) {
        NodeType node = nodeExists(var.name);
        if (node == null) {
            reportError("Error: Variable " + var.name + " is not declared at line " + (var.row + 1));
        }
    }

    @Override
    public void visit(VarDecList varDecList, int level) {
        while (varDecList != null) {
            if (varDecList.head != null) {
                varDecList.head.accept(this, level);
            }
            varDecList = varDecList.tail;
        }
    }

    @Override
    public void visit(WhileExp exp, int level) {
        // *** FIX for "test conditions are int/bool"
        int type = evaluateExpression(exp.test);
        if (type != 0 && type != 1) {
            reportError("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1)
                        + ": While condition must be boolean or int");
        }
        indent(level);
        System.out.println("Entering a while loop:");
        if (exp.body != null) {
            exp.body.accept(this, level + 1);
        }
        indent(level);
        System.out.println("Leaving the while loop");
    }

    @Override
    public void visit(ReturnExp exp, int level) {
        if (exp.returnValue != null) {
            exp.returnValue.accept(this, level);
        }
    }

    @Override
    public void visit(CallExp exp, int level) {
        // *** FIX for "Unmatched parameter number/types"
        NodeType node = functionExists(exp.func);
        if (node == null) {
            reportError("Error: Function " + exp.func + " is not declared at line " + (exp.row + 1));
            return;
        }
        if (!(node.def instanceof FunctionDec)) return;
        FunctionDec fdec = (FunctionDec) node.def;

        // Count formal parameters
        int paramCount = 0;
        VarDecList p = fdec.params;
        while (p != null) {
            paramCount++;
            p = p.tail;
        }

        // Count actual arguments
        int argCount = 0;
        ExpList a = exp.args;
        while (a != null) {
            argCount++;
            a = a.tail;
        }

        if (argCount != paramCount) {
            reportError("Error in line " + (exp.row + 1) + ": Function '"
                        + exp.func + "' expects " + paramCount
                        + " arguments, got " + argCount);
        } else {
            // Type check each parameter vs argument
            VarDecList paramList = fdec.params;
            ExpList argList = exp.args;
            int i = 1;
            while (paramList != null && argList != null) {
                int paramType = -1;
                if (paramList.head instanceof SimpleDec) {
                    paramType = ((SimpleDec) paramList.head).typeSpecifier.typ;
                } else if (paramList.head instanceof ArrayDec) {
                    paramType = ((ArrayDec) paramList.head).typ.typ;
                }
                int argType = evaluateExpression(argList.head);
                if (paramType != argType && paramType != -1 && argType != -1) {
                    reportError("Error in line " + (exp.row + 1)
                                + ": Parameter " + i + " of function '"
                                + exp.func + "' type mismatch");
                }
                paramList = paramList.tail;
                argList = argList.tail;
                i++;
            }
        }
    }

    @Override
    public void visit(OpExp exp, int level) {
        // *** FIX for "two sides of an operation"
        int leftType = evaluateExpression(exp.left);
        int rightType = evaluateExpression(exp.right);
        if (leftType == -1 || rightType == -1) {
            return; // error already flagged
        }
        if (leftType != rightType) {
            reportError("Error in line " + (exp.row + 1) + ", column " + (exp.col + 1)
                        + ": Operand types do not match");
        }
        // Additional boolean checks if the operator is AND/OR in evaluateExpression

    }

    @Override
    public void visit(IntExp exp, int level) {
        // No semantic checks needed for int literal
    }

    @Override
    public void visit(BoolExp exp, int level) {
        // No semantic checks needed for bool literal
    }

    @Override
    public void visit(IndexVar var, int level) {
        NodeType node = nodeExists(var.name);
        if (node == null) {
            reportError("Error: Array " + var.name + " is not declared at line " + (var.row + 1));
        } else {
            // *** FIX for array index must be int
            int idxType = evaluateExpression(var.index);
            if (idxType != 1) {
                reportError("Error in line " + (var.row + 1) + ": Array index must be int");
            }
        }
    }

    /**
     * Print the symbol table for the current scope.
     */
    public void printSymbolTable(int level) {
        ArrayList<NodeType> scope = symbolTable.get(scopeStack.peek());
        if (scope != null) {
            for (NodeType node : scope) {
                indent(level);
                if (node.def instanceof SimpleDec) {
                    SimpleDec dec = (SimpleDec) node.def;
                    System.out.println(node.name + ": " + getTypeName(dec.typeSpecifier.typ));
                } else if (node.def instanceof ArrayDec) {
                    ArrayDec dec = (ArrayDec) node.def;
                    System.out.println(node.name + ": " + getTypeName(dec.typ.typ) + "[" + dec.size + "]");
                } else if (node.def instanceof FunctionDec) {
                    FunctionDec dec = (FunctionDec) node.def;
                    System.out.println(node.name + ": (" + getParamTypes(dec.params) + ") -> "
                                       + getTypeName(dec.result.typ));
                } else {
                    System.out.println(node.name + ": " + node.def.toString());
                }
            }
        }
    }

    @Override
    public void visit(SimpleDec dec, int level) {
        // *** FIX for "No name (-2), No type (-2)"
        if (dec.identifier == null || dec.identifier.isEmpty()) {
            reportError("Error: Simple variable has no name at line " + (dec.row + 1));
            return;
        }
        if (dec.typeSpecifier == null) {
            reportError("Error: Simple variable '" + dec.identifier + "' has no type at line " + (dec.row + 1));
            return;
        }
        // *** FIX for "Didn't capture redefined"
        ArrayList<NodeType> topScope = symbolTable.get(scopeStack.peek());
        if (topScope != null) {
            for (NodeType n : topScope) {
                if (n.name.equals(dec.identifier)) {
                    reportError("Error: Variable '" + dec.identifier + "' already declared in this scope at line "
                                + (dec.row + 1));
                    return;
                }
            }
        }
        NodeType node = new NodeType(dec.identifier, dec, scopeStack.peek());
        insert(scopeStack.peek(), node);
    }

    @Override
    public void visit(NilExp exp, int level) {
        // no checks
    }

    @Override
    public void visit(NameTy type, int level) {
        // no checks
    }

    @Override
    public void visit(IfExp exp, int level) {
        if (exp.test != null) {
            exp.test.accept(this, level);
            int condType = evaluateExpression(exp.test);
            // *** FIX for "test conditions are int/bool"
            if (condType != 0 && condType != 1) {
                reportError("Error in line " + (exp.row + 1) + ": If condition must be boolean or int");
            }
        }
        indent(level);
        System.out.println("Entering if statement:");
        if (exp.then != null) {
            exp.then.accept(this, level + 1);
        }
        if (exp.elsee != null) {
            indent(level);
            System.out.println("Entering else statement:");
            exp.elsee.accept(this, level + 1);
            indent(level);
            System.out.println("Leaving else statement");
        }
        indent(level);
        System.out.println("Leaving if statement");
    }

    @Override
    public void visit(FunctionDec dec, int level) {
        // *** FIX for "No name (-2), No function return-type (-2)"
        if (dec.func == null || dec.func.isEmpty()) {
            reportError("Error in line " + (dec.row + 1) + ": Function has no name");
        }
        if (dec.result == null) {
            reportError("Error in line " + (dec.row + 1) + ": Function '" + dec.func + "' has no return type");
        }
        NodeType existing = functionExists(dec.func);
        boolean isRedefinition = false;
        if (existing != null) {
            FunctionDec existingFunc = (FunctionDec) existing.def;
            boolean existingIsPrototype = (existingFunc.body == null);
            boolean currentIsPrototype = (dec.body == null);
            // If both are either full definitions or both are prototypes => redefinition
            if (existingIsPrototype == currentIsPrototype) {
                reportError("Error in line " + (dec.row + 1)
                            + ": Function '" + dec.func + "' already declared");
                isRedefinition = true;
            }
        }
        if (!isRedefinition) {
            NodeType node = new NodeType(dec.func, dec, "global");
            if (existing != null) {
                existing.def = dec;
            } else {
                insert("global", node);
            }
        }
        // *** FIX for "no function parameters (-2), no entry/exit structure (-2)"
        nestLevel++;
        String scopeName = "function_" + dec.func;
        scopeStack.push(scopeName);

        indent(level);
        System.out.println("Entering the scope for function " + dec.func + ":");
        if (dec.params != null) {
            visitFunctionParams(dec.params, level + 1);
        }
        if (dec.body != null) {
            dec.body.accept(this, level + 1);
        }
        printSymbolTable(level + 1);
        indent(level);
        String returnType = (dec.result == null) ? "unknown" : getTypeName(dec.result.typ);
        System.out.println("Leaving the function scope " + dec.func + ": ("
                           + getParamTypes(dec.params) + ") -> " + returnType);
        scopeStack.pop();
    }

    private void visitFunctionParams(VarDecList varDecList, int level) {
        while (varDecList != null) {
            if (varDecList.head != null) {
                if (varDecList.head instanceof SimpleDec) {
                    SimpleDec dec = (SimpleDec) varDecList.head;
                    if (dec.identifier == null || dec.identifier.isEmpty()) {
                        reportError("Error: Parameter has no name at line " + (dec.row + 1));
                    }
                    if (dec.typeSpecifier == null) {
                        reportError("Error: Parameter '" + dec.identifier
                                    + "' has no type at line " + (dec.row + 1));
                    }
                    NodeType node = new NodeType(dec.identifier, dec, scopeStack.peek());
                    insert(scopeStack.peek(), node);
                } else if (varDecList.head instanceof ArrayDec) {
                    ArrayDec arr = (ArrayDec) varDecList.head;
                    if (arr.name == null || arr.name.isEmpty()) {
                        reportError("Error: Parameter array has no name at line " + (arr.row + 1));
                    }
                    if (arr.typ == null) {
                        reportError("Error: Parameter array '" + arr.name
                                    + "' has no type at line " + (arr.row + 1));
                    }
                    NodeType node = new NodeType(arr.name, arr, scopeStack.peek());
                    insert(scopeStack.peek(), node);
                }
            }
            varDecList = varDecList.tail;
        }
    }

    @Override
    public void visit(ExpList expList, int level) {
        while (expList != null) {
            if (expList.head != null) {
                expList.head.accept(this, level);
            }
            expList = expList.tail;
        }
    }

    @Override
    public void visit(DecList decList, int level) {
        while (decList != null) {
            if (decList.head != null) {
                decList.head.accept(this, level);
            }
            decList = decList.tail;
        }
    }

    @Override
    public void visit(CompoundExp exp, int level) {
        // *** FIX for function blocks or local blocks => new scope
        nestLevel++;
        String scopeName = "scope_" + nestLevel;
        scopeStack.push(scopeName);
        if (exp.decs != null) {
            visit(exp.decs, level + 1);
        }
        if (exp.exps != null) {
            visit(exp.exps, level + 1);
        }
        printSymbolTable(level + 1);
        scopeStack.pop();
    }

    @Override
    public void visit(AssignExp exp, int level) {
        // Left side
        if (exp.lhs != null) {
            exp.lhs.accept(this, level);
        }
        // Right side
        if (exp.rhs != null) {
            exp.rhs.accept(this, level);
        }
        // *** FIX for "two sides of an assignment"
        if (exp.lhs instanceof VarExp) {
            int lhsType = varType((VarExp) exp.lhs);
            int rhsType = evaluateExpression(exp.rhs);
            if (lhsType != rhsType && lhsType != -1 && rhsType != -1) {
                reportError("Error in line " + (exp.row + 1) + ": Type mismatch in assignment");
            }
        }
    }

    @Override
    public void visit(ArrayDec dec, int level) {
        // *** FIX for "No name (-2), No type (-2); no size (-2)"
        if (dec.name == null || dec.name.isEmpty()) {
            reportError("Error: Array has no name at line " + (dec.row + 1));
            return;
        }
        if (dec.typ == null) {
            reportError("Error: Array '" + dec.name + "' has no type at line " + (dec.row + 1));
        }
        if (dec.size <= 0) {
            reportError("Error: Array '" + dec.name
                        + "' must have a positive size at line " + (dec.row + 1));
        }
        // *** FIX for "didn't capture redefined"
        ArrayList<NodeType> topScope = symbolTable.get(scopeStack.peek());
        if (topScope != null) {
            for (NodeType n : topScope) {
                if (n.name.equals(dec.name)) {
                    reportError("Error: Array '" + dec.name
                                + "' already declared in this scope at line " + (dec.row + 1));
                    return;
                }
            }
        }
        NodeType node = new NodeType(dec.name, dec, scopeStack.peek());
        insert(scopeStack.peek(), node);
    }
    /**
     * Start the analysis from the global scope
     */
    public void analyze(DecList program) {
        System.out.println("Entering the global scope:");
        if (program != null) {
            visit(program, 1);
        }
        printSymbolTable(1);
        System.out.println("Leaving the global scope");
    }

    /**
     * Convert our internal structure to a simpler SymbolTable for code generation.
     */
    public SymbolTable getSymbolTable() {
        SymbolTable table = new SymbolTable();
        ArrayList<NodeType> nodes = symbolTable.get("global");
        if (nodes != null) {
            for (NodeType node : nodes) {
                if (node.def instanceof SimpleDec) {
                    table.insert(node.name, new VarSymbolInfo(0, true));
                } else if (node.def instanceof ArrayDec) {
                    ArrayDec arr = (ArrayDec) node.def;
                    table.insert(node.name, new ArraySymbolInfo(0, arr.size, true));
                } else if (node.def instanceof FunctionDec) {
                    table.insert(node.name, new FunctionSymbolInfo(0, true));
                }
            }
        }
        return table;
    }
}