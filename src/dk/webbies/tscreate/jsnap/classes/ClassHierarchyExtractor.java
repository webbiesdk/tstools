package dk.webbies.tscreate.jsnap.classes;

import dk.webbies.tscreate.jsnap.Snap;

import java.util.*;

/**
 * Created by webbies on 31-08-2015.
 */
public class ClassHierarchyExtractor {
    private Snap.Obj globalObject;

    public ClassHierarchyExtractor(Snap.Obj globalObject) {
        this.globalObject = globalObject;
    }

    private List<Snap.Obj> extractClasses(String prefixPath, Snap.Obj obj, Map<Snap.Obj, LibraryClass> classes, Set<Snap.Obj> seenObjects) {
        if (seenObjects.contains(obj)) {
            Snap.Property prototype = obj.getProperty("prototype");
            if (prototype != null && prototype.value != null && prototype.value instanceof Snap.Obj) {
                LibraryClass klass = classes.get(prototype.value);
                if (klass != null && !klass.pathsSeen.contains(prefixPath)) {
                    klass.pathsSeen.add(prefixPath);
                }
            }
            return Collections.EMPTY_LIST;
        }
        seenObjects.add(obj);
        ArrayList<Snap.Obj> missingEnvs = new ArrayList<>();

        if (obj.function != null && obj.properties != null) {
            Snap.Property prototypeProperty = obj.getProperty("prototype");
            if (prototypeProperty != null && prototypeProperty.value instanceof Snap.Obj) {
                createLibraryClass(prefixPath, obj, classes);
            }
        }

        if (obj.properties != null) {
            for (Snap.Property property : obj.properties) {
                if (property.value instanceof Snap.Obj) {
                    missingEnvs.addAll(extractClasses(prefixPath + "." + property.name, (Snap.Obj) property.value, classes, seenObjects));
                }
            }
        }

        if (obj.prototype != null) {
            for (Snap.Property property : obj.prototype.properties) {
                if (property.value instanceof Snap.Obj) {
                    missingEnvs.addAll(extractClasses(prefixPath + ".[prototype]." + property.name, (Snap.Obj) property.value, classes, seenObjects));
                }
            }
        }


        if (obj.env != null) {
            missingEnvs.add(obj.env);
        }
        return missingEnvs;
    }

    private void createLibraryClass(String path, Snap.Obj obj, Map<Snap.Obj, LibraryClass> classes) {
        if (obj == null) {
            return;
        }
        Snap.Obj prototype = (Snap.Obj) obj.getProperty("prototype").value;

        if (classes.containsKey(prototype)) {
            return;
        }
        protoTypeToClass(path, classes, prototype);
    }

    private LibraryClass protoTypeToClass(String path, Map<Snap.Obj, LibraryClass> classes, Snap.Obj prototype) {
        if (prototype == null) {
            return null;
        }
        // This is the case when we are dealing with Object.prototype, since it doesn't have any super-class.
        if (prototype.prototype == null) {
            return null;
        }
        if (classes.containsKey(prototype)) {
            return classes.get(prototype);
        }
        LibraryClass libraryClass = new LibraryClass(path, prototype);
        if (prototype.properties.size() > 1) {
            libraryClass.isUsedAsClass = true;
        }
        classes.put(prototype, libraryClass);

        libraryClass.superClass = protoTypeToClass(path + ".[proto]", classes, prototype.prototype);

        return libraryClass;
    }

    public HashMap<Snap.Obj, LibraryClass> extract() {
        HashMap<Snap.Obj, LibraryClass> libraryClasses = new HashMap<>();
        HashSet<Snap.Obj> seen = new HashSet<>();
        // Reason for two passes: Names look prettier when we don't go through the environment.
        List<Snap.Obj> missingEnvs = this.extractClasses("window", this.globalObject, libraryClasses, seen);

        for (Snap.Obj missinEnv : missingEnvs) {
            for (Snap.Property property : missinEnv.properties) {
                if (property.value instanceof Snap.Obj) {
                    Snap.Obj obj = (Snap.Obj) property.value;
                    extractClasses("[ENV]." + property.name, obj, libraryClasses, seen);
                }
            }
        }

        new MarkClassUsage(libraryClasses).visit(this.globalObject);

        return libraryClasses;
    }

    private static final class MarkClassUsage {
        private final HashSet<Snap.Obj> seen = new HashSet<>();
        private final HashMap<Snap.Obj, LibraryClass> libraryClasses;

        public MarkClassUsage(HashMap<Snap.Obj, LibraryClass> libraryClasses) {
            this.libraryClasses = libraryClasses;
        }


        public void visit(Snap.Obj obj) {
            if (seen.contains(obj)) {
                return;
            }
            seen.add(obj);
            if (obj.prototype != null) {
                LibraryClass libraryClass = libraryClasses.get(obj.prototype);
                if (libraryClass != null) {
                    libraryClass.isUsedAsClass = true;
                }
            }

            for (Snap.Property prop : obj.getPropertyMap().values()) {
                visitProp(prop);
            }

            if (obj.env != null) {
                for (Snap.Property prop : obj.env.getPropertyMap().values()) {
                    visitProp(prop);
                }
            }

            if (obj.prototype != null && obj.prototype.properties != null) {
                for (Snap.Property prop : obj.prototype.getPropertyMap().values()) {
                    visitProp(prop);
                }
            }
        }

        private void visitProp(Snap.Property prop) {
            if (prop.value instanceof Snap.Obj) {
                visit((Snap.Obj) prop.value);
            }
            if (prop.get != null && !(prop.get instanceof Snap.UndefinedConstant)) {
                visit((Snap.Obj) prop.get);
            }
            if (prop.set != null && !(prop.set instanceof Snap.UndefinedConstant)) {
                visit((Snap.Obj) prop.set);
            }
        }
    }
}
