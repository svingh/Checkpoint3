package absyn;

abstract public class Exp extends Absyn {
    public int getType() {
        return -1; // Default implementation, subclasses should override this
    }
}