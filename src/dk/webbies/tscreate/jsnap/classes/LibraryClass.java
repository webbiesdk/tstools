package dk.webbies.tscreate.jsnap.classes;

import dk.webbies.tscreate.analysis.unionFind.nodes.EmptyUnionNode;
import dk.webbies.tscreate.analysis.unionFind.nodes.UnionNode;
import dk.webbies.tscreate.jsnap.Snap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by webbies on 02-09-2015.
 */
public class LibraryClass {
    public List<String> pathsSeen = new ArrayList<>();
    public LibraryClass superClass;
    private String name = null;

    public final UnionNode constructorNode = new EmptyUnionNode();

    public final UnionNode thisNode = new EmptyUnionNode();

    public boolean isUsedAsClass = false; // All functions are potential library classes, this marks if it is actually used as a class.

    public Snap.Obj prototype;

    public LibraryClass(String pathSeen, Snap.Obj prototype) {
        this.pathsSeen.add(pathSeen);
        this.prototype = prototype;
    }

    // TODO: Somehow make sure that this doesn't conflict.
    public String getName() {
        if (name == null) {
            name = getNameFromPath(pathsSeen.get(0));
            // Finding the longest name, that preferably starts with an upper case.
            for (int i = 1; i < pathsSeen.size(); i++) {
                String path = pathsSeen.get(i);
                String newName = getNameFromPath(path);
                if (newName.equals("constructor") || newName.equals("[proto]")) {
                    continue;
                }

                boolean newIsUpper = newName.charAt(0) == Character.toUpperCase(newName.charAt(0));
                boolean oldIsUpper = name.charAt(0) == Character.toUpperCase(name.charAt(0));

                if (newIsUpper && !oldIsUpper) {
                    name = newName;
                }
                if (newName.length() > name.length() && !(!newIsUpper && oldIsUpper)) {
                    name = newName;
                }
            }
        }
        return name;
    }

    private String getNameFromPath(String path) {
        String[] split = path.split("\\.");
        return split[split.length - 1];
    }

    public boolean isPrimitiveClass() {
        switch (this.getName()) {
            case "Number":
            case "String":
            case "Boolean":
            case "Function":
                return true;
            default:
                return false;
        }
    }
}
