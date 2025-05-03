// In SymbolInfo.java
public class SymbolInfo {  // Not Symbol
    private int offset;
    private boolean isGlobal;
    
    public SymbolInfo(int offset, boolean isGlobal) {
        this.offset = offset;
        this.isGlobal = isGlobal;
    }
    
    public int getOffset() { return offset; }
    public boolean isGlobal() { return isGlobal; }
}