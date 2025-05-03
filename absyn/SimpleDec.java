package absyn;

public class SimpleDec extends VarDec {
    public NameTy typeSpecifier;  
    public String identifier;     
    
    public SimpleDec(int row, int col, NameTy typeSpecifier, String identifier) {
        this.row = row;
        this.col = col;
        this.typeSpecifier = typeSpecifier;
        this.identifier = identifier;
    }
    
    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }

    @Override
    public int getType() {
        return typeSpecifier.typ;
    }
}