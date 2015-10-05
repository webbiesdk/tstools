package dk.webbies.tscreate.analysis.unionFind;

/**
 * Created by Erik Krogh Kristensen on 02-09-2015.
 */
public abstract class UnionNode {
    UnionNode parent;
    int rank = 0;
    UnionClass unionClass;

    private static int instanceCounter = 0;
    private final int counter;
    public UnionNode() {
        this.counter = instanceCounter++;
        if (this.counter == -1) {
            throw new RuntimeException(); // Just so i have a breakPoint.
        }
    }

    public UnionClass getUnionClass() {
        if (parent == null) {
            return null;
        }
        return findParent().unionClass;
    }

    private UnionNode findParent() {
        if (this.parent == null) {
            throw new RuntimeException();
        }
        while (this.parent != this.parent.parent) {
            this.parent = this.parent.parent;
        }
        return this.parent;
    }
}