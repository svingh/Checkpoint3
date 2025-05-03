import absyn.*;



public class NodeType {
    public String name;
    public absyn.Absyn def;
    public String scope;
    
    public NodeType(String name, absyn.Absyn def, String scope) {
        this.name = name;
        this.def = def;
        this.scope = scope;
    }
}