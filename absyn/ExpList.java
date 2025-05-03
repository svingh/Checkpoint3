package absyn;

public class ExpList {
    public Exp head;
    public ExpList tail;

    public ExpList(Exp head, ExpList tail) {
        this.head = head;
        this.tail = tail;
    }

    public void appendExp(Exp exp) {
        if (this.tail == null) {
            this.tail = new ExpList(exp, null);
        } else {
            this.tail.appendExp(exp);
        }
    }
}

