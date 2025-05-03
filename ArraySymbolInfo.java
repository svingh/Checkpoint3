public class ArraySymbolInfo extends SymbolInfo {
    private int size;
    
    public ArraySymbolInfo(int offset, int size, boolean isGlobal) {
        super(offset, isGlobal);
        this.size = size;
    }
    
    public int getSize() { return size; }
}