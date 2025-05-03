package absyn;

public class VarDecList {
    public VarDec head;
    public VarDecList tail;

    public VarDecList(VarDec head, VarDecList tail) {
        this.head = head;
        this.tail = tail;
    }

    public void appendVarDec(VarDec vd) {
        if (this.tail == null) {
            this.tail = new VarDecList(vd, null);
        } else {
            this.tail.appendVarDec(vd);
        }
    }
}