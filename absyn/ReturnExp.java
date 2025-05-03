package absyn;

public class ReturnExp extends Exp {
    public Exp returnValue;
    
    public ReturnExp(int row, int col, Exp returnValue) {
        this.row = row;
        this.col = col;
        this.returnValue = returnValue;
    }
    
    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }
}