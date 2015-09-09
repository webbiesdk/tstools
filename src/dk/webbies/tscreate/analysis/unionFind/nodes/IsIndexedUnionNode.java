package dk.webbies.tscreate.analysis.unionFind.nodes;

/**
 * Created by Erik Krogh Kristensen on 09-09-2015.
 */
public class IsIndexedUnionNode extends UnionNodeWithFields {
    private final UnionNode returnType;
    private final UnionNode lookupExp;

    public IsIndexedUnionNode(UnionNode returnType, UnionNode lookupExp) {
        this.returnType = returnType;
        addField("isIndexer-returnType", returnType);
        this.lookupExp = lookupExp;
        addField("isIndexer-lookupExp", lookupExp);
    }

    public UnionNode getReturnType() {
        return returnType;
    }

    public UnionNode getLookupExp() {
        return lookupExp;
    }
}
