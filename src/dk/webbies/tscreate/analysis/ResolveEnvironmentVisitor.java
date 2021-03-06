package dk.webbies.tscreate.analysis;

import dk.webbies.tscreate.Options;
import dk.webbies.tscreate.analysis.unionFind.*;
import dk.webbies.tscreate.jsnap.Snap;
import dk.webbies.tscreate.jsnap.classes.LibraryClass;
import dk.webbies.tscreate.paser.AST.FunctionExpression;
import dk.webbies.tscreate.paser.AST.Identifier;
import dk.webbies.tscreate.paser.AST.NodeTransverse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Erik Krogh Kristensen on 05-09-2015.
 */
public class ResolveEnvironmentVisitor implements NodeTransverse<Void> {
    private final Snap.Obj closure;
    private final FunctionExpression function;
    private final UnionFindSolver solver;
    private final Map<Identifier, UnionNode> identifierMap;
    private final Snap.Obj globalObject;
    private final Map<String, Snap.Property> globalValues;
    private final Map<String, Snap.Property> values;
    private final PrimitiveNode.Factory primitiveFactory;
    private final HeapValueFactory heapFactory;
    private final Map<Snap.Obj, LibraryClass> libraryClasses;
    private final Options options;

    public ResolveEnvironmentVisitor(
            Snap.Obj closure,
            FunctionExpression function,
            UnionFindSolver solver,
            Map<Identifier, UnionNode> identifierMap,
            Map<String, Snap.Property> values,
            Map<String, Snap.Property> globalValues,
            Snap.Obj globalObject,
            HeapValueFactory heapFactory,
            Map<Snap.Obj, LibraryClass> libraryClasses,
            Options options) {
        this.closure = closure;
        this.function = function;
        this.solver = solver;
        this.identifierMap = identifierMap;
        this.globalObject = globalObject;
        this.heapFactory = heapFactory;
        this.libraryClasses = libraryClasses;
        this.options = options;
        this.globalValues = new HashMap<>(globalValues);
        this.values = new HashMap<>(values);
        this.primitiveFactory = heapFactory.getPrimitivesFactory();
        function.declarations.keySet().forEach(this.values::remove);
        function.declarations.keySet().forEach(this.globalValues::remove);

    }

    @Override
    public Void visit(FunctionExpression function) {
        if (function != this.function) {
            new ResolveEnvironmentVisitor(this.closure, function, this.solver, this.identifierMap, this.values, this.globalValues, globalObject, heapFactory, libraryClasses, options).visit(function);
            return null;
        } else {
            return NodeTransverse.super.visit(function);
        }
    }

    @Override
    public Void visit(Identifier identifier) {
        String name = identifier.getName();
        UnionNode idNode = getIdentifier(identifier, solver, identifierMap);
        if (this.values.containsKey(name)) {
            solver.union(idNode, heapFactory.fromProperty(this.values.get(name)));
        } else if (identifier.isGlobal) {
            if (name.equals("arguments")) {
                solver.union(idNode, new DynamicAccessNode(solver, primitiveFactory.any(), primitiveFactory.number()));
                ObjectNode obj = new ObjectNode(solver);
                obj.addField("length", primitiveFactory.number());
                solver.union(idNode, obj);
            } else if (this.globalValues.containsKey(name)) {
                solver.union(idNode, heapFactory.fromProperty(this.globalValues.get(name)));
            } else {
                solver.union(idNode, primitiveFactory.any());
            }
        } else {
            assert identifier.getDeclaration() != null;
            if (!options.unionHeapIdentifiers) {
                UnionNode declaration = getIdentifier(identifier.getDeclaration(), solver, identifierMap);
                solver.union(declaration, idNode);
            }
        }

        return NodeTransverse.super.visit(identifier);
    }

    public static UnionNode getIdentifier(Identifier identifier, UnionFindSolver solver, Map<Identifier, UnionNode> identifierMap) {
        if (!identifierMap.containsKey(identifier)) {
            identifierMap.put(identifier, new EmptyNode(solver));
        }
        return identifierMap.get(identifier);
    }
}
