package dk.webbies.tscreate.paser.AST;

/**
 * Created by hamid on 10/25/15.
 * Used for no operation (e.g. function entry) and branches
 */
public class CFGEntry extends CFGNode {
    public String toString() { return "ENTRY:" + this.hashCode(); }
}