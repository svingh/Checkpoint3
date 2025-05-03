package absyn;

public class VarExp extends Exp {
    public Var variable;

    public VarExp(int row, int col, Var variable) {
        this.row = row;
        this.col = col;
        this.variable = variable;
    }

    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }

    @Override
    public int getType() {
        return -1; // The type should be checked in SemanticAnalyzer
    }
}