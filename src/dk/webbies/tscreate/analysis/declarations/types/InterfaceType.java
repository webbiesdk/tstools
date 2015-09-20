package dk.webbies.tscreate.analysis.declarations.types;

import com.google.javascript.jscomp.newtypes.Declaration;

/**
 * Created by Erik Krogh Kristensen on 08-09-2015.
 */
public class InterfaceType implements DeclarationType {
    private int useCounter = 0;
    public DeclarationType function = null;
    public DeclarationType object = null;

    public final String name;

    public InterfaceType(String name) {
        // TODO: Ensure no conflicts
        this.name = name;
    }

    public void incrementUseCounter() {
        this.useCounter++;
    }

    // TODO: Use this.
    public int getUseCounter() {
        return useCounter;
    }

    // TODO: Unresolved.
    public FunctionType getFunction() {
        if (function == null && object == null) {
            throw new NullPointerException("An interface must have either an object or function associated");
        }
        return (FunctionType) function;
    }

    public ObjectType getObject() {
        if (function == null && object == null) {
            throw new NullPointerException("An interface must have either an object or function associated");
        }
        return (ObjectType) object;
    }

    @Override
    public <T> T accept(DeclarationTypeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
