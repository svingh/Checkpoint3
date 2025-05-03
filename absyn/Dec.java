package absyn;

abstract public class Dec extends Absyn {
    public int getType() {
        return -1; // Default implementation, override in subclasses
    }
}