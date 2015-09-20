package dk.webbies.tscreate.analysis.unionFind.nodes;

import dk.webbies.tscreate.jsnap.Snap;
import dk.webbies.tscreate.paser.AST.FunctionExpression;
import dk.webbies.tscreate.paser.AST.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Erik Krogh Kristensen on 02-09-2015.
 */
public class FunctionNode extends UnionNodeWithFields {
    public final UnionNode returnNode;
    public final List<UnionNode> arguments = new ArrayList<>();
    public FunctionExpression astFunction;
    public final UnionNode thisNode;
    public Snap.Obj closure = null;

    private final List<String> argumentNames;

    public boolean hasAnalyzed = false; // For when analysing the functions separately.

    private static int instanceCounter = 0;
    public final int counter;

    public FunctionNode(List<String> argumentNames) {
        this.counter = instanceCounter++;
        this.argumentNames = argumentNames;
        this.returnNode = new EmptyUnionNode();
        this.thisNode = new EmptyUnionNode();
        for (int i = 0; i < argumentNames.size(); i++) {
            EmptyUnionNode node = new EmptyUnionNode();
            arguments.add(node);
            addField("function-argument-" + i, node);
        }
        addField("function-return", returnNode);
        addField("function-this", thisNode);
    }

    public List<String> getArgumentNames() {
        return argumentNames;
    }

    public FunctionNode(FunctionExpression function) {
        this(function.getArguments().stream().map(Identifier::getName).collect(Collectors.toList()));
        this.astFunction = function;
    }

    public FunctionNode(Snap.Obj closure) {
        this(closure.function.astNode);
        this.closure = closure;
    }

    public FunctionNode(Snap.Obj closure, List<String> argumentNames) {
        this(argumentNames);
        this.closure = closure;
    }
}
