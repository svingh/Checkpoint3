import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, SymbolInfo> table = new HashMap<>();
    
    public void insert(String name, SymbolInfo symbol) {
        table.put(name, symbol);
    }
    
    public SymbolInfo lookup(String name) {
        return table.get(name);
    }
}