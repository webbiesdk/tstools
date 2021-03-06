package dk.webbies.tscreate.analysis.unionFind;

import dk.webbies.tscreate.jsnap.Snap;

import java.util.HashSet;

/**
 * Created by Erik Krogh Kristensen on 09-09-2015.
 */
public class HasPrototypeNode extends UnionNode {
    private Snap.Obj prototype;

    public HasPrototypeNode(UnionFindSolver solver, Snap.Obj prototype) {
        super(solver);
        if (prototype == null) {
            throw new RuntimeException();
        }
        this.prototype = prototype;
    }

    public Snap.Obj getPrototype() {
        return prototype;
    }

    @Override
    public void addTo(UnionClass unionClass) {
        UnionFeature feature = unionClass.getFeature();
        if (feature.prototypes == null) {
            feature.prototypes = new HashSet<>();
        }
        feature.prototypes.add(this.prototype);
    }
}
