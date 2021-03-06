package dk.webbies.tscreate.paser;


import com.google.javascript.jscomp.parsing.Config;
import com.google.javascript.jscomp.parsing.Config.LanguageMode;
import com.google.javascript.jscomp.parsing.ConfigExposer;
import com.google.javascript.jscomp.parsing.ParserRunner;
import com.google.javascript.jscomp.parsing.parser.Parser;
import com.google.javascript.jscomp.parsing.parser.Parser.Config.Mode;
import com.google.javascript.jscomp.parsing.parser.SourceFile;
import com.google.javascript.jscomp.parsing.parser.trees.Comment;
import com.google.javascript.jscomp.parsing.parser.trees.ProgramTree;
import com.google.javascript.jscomp.parsing.parser.util.SourcePosition;
import com.google.javascript.jscomp.parsing.parser.util.SourceRange;
import com.google.javascript.rhino.ErrorReporter;
import dk.webbies.tscreate.main.Main;
import dk.webbies.tscreate.declarationReader.DeclarationParser;
import dk.webbies.tscreate.paser.AST.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JavaScript parser.
 * Based on the parser from the Google Closure Compiler.
 *
 * And this is based on the Google Closure Compiler interface in TAJS.
 */
public class JavaScriptParser {
    private final Mode mode;
    private final Config config;
    private final DeclarationParser.Environment environment;

    /**
     * Constructs a new parser.
     * @param languageMode
     */
    public JavaScriptParser(Main.LanguageLevel languageMode) {
        this.mode = languageMode.closureCompilerMode;
        this.environment = languageMode.environment;
        LanguageMode m;
        switch (mode) {
            case ES3:
                m = LanguageMode.ECMASCRIPT3;
                break;
            case ES5:
                m = LanguageMode.ECMASCRIPT5;
                break;
            case ES5_STRICT:
                m = LanguageMode.ECMASCRIPT5_STRICT;
                break;
            case ES6:
                m = LanguageMode.ECMASCRIPT6;
                break;
            case ES6_STRICT:
                m = LanguageMode.ECMASCRIPT6_STRICT;
                break;
            default:
                throw new RuntimeException("Unexpected enum: " + mode);
        }
        config = ConfigExposer.createConfig(new HashSet<>(), new HashSet<>(), true, false, m);
    }

    /**
     * Parses the given JavaScript code.
     * The syntax check includes break/continue label consistency and no duplicate parameters.
     *
     * @param name     file name or URL of the code
     * @param contents the code
     *
     *                 new ErrorReporter() {
     */
    public ParseResult parse(String name, String contents) {
        final List<SyntaxMesssage> warnings = new ArrayList<>();
        final List<SyntaxMesssage> errors = new ArrayList<>();
        ParserRunner.parse(new com.google.javascript.jscomp.SourceFile(name), contents, config, new ErrorReporter() {
            @Override
            public void warning(String message, String name2, int lineNumber, int columnNumber) {
                warnings.add(new SyntaxMesssage(message, new SourceLocation(lineNumber, columnNumber + 1, name2)));
            }

            @Override
            public void error(String message, String name2, int lineNumber, int columnNumber) {
                errors.add(new SyntaxMesssage(message, new SourceLocation(lineNumber, columnNumber + 1, name2)));
            }
        });
        ProgramTree programAST = null;
        if (errors.isEmpty()) {
            programAST = new Parser(new Parser.Config(mode), new MutedErrorReporter(), new SourceFile(name, contents)).parseProgram();
        }
        return new ParseResult(programAST, errors, warnings, environment);
    }

    /**
     * Syntax error message.
     */
    static class SyntaxMesssage {

        private final String message;

        private final SourceLocation sourceLocation;

        /**
         * Constructs a new syntax error message object.
         */
        SyntaxMesssage(String message, SourceLocation sourceLocation) {
            this.message = message;
            this.sourceLocation = sourceLocation;
        }

        /**
         * Returns the message.
         */
        String getMessage() {
            return message;
        }

        /**
         * Returns the source location.
         */
        SourceLocation getSourceLocation() {
            return sourceLocation;
        }
    }

    /**
     * Result from parser.
     */
    public static class ParseResult {

        private ProgramTree programAST;

        private final List<SyntaxMesssage> errors;

        private final List<SyntaxMesssage> warnings;
        private DeclarationParser.Environment environment;

        private ParseResult(ProgramTree programAST, List<SyntaxMesssage> errors, List<SyntaxMesssage> warnings, DeclarationParser.Environment environment) {
            this.programAST = programAST;
            this.errors = errors;
            this.warnings = warnings;
            this.environment = environment;
        }

        /**
         * Returns the AST, or null if parse error.
         */
        ProgramTree getProgramAST() {
            return programAST;
        }

        /**
         * Returns the list of parse errors.
         */
        List<SyntaxMesssage> getErrors() {
            return errors;
        }

        /**
         * Returns the list of parse warnings.
         */
        List<SyntaxMesssage> getWarnings() {
            return warnings;
        }

        public FunctionExpression toTSCreateAST() {
            AstTransformer transformer = new AstTransformer(environment);
            List<Statement> body = this.programAST.sourceElements.stream().map(transformer::convert).map(JavaScriptParser::toStatement).collect(Collectors.toList());
            if (body.isEmpty()) {
                SourcePosition position = new SourcePosition(new SourceFile("empty", ""), 0, 0, 0);
                SourceRange location = new SourceRange(position, position);
                return new FunctionExpression(location, new Identifier(location, ":program"), new BlockStatement(location, Collections.EMPTY_LIST), Collections.EMPTY_LIST);
            } else {
                SourceRange location = new SourceRange(body.get(0).location.start, body.get(body.size() - 1).location.end);
                FunctionExpression result = new FunctionExpression(location, new Identifier(location, ":program"), new BlockStatement(location, body), Collections.EMPTY_LIST);

                // For each function, mark which variables it declares in its scope.
                new FillFunctionsVariableDeclarations(result).visit(result);
                // For each identifier, mark where it was declared.
                new FindVariableDeclarations(result).visit(result);

                FindJSDocVisitor jsDocVisitor = new FindJSDocVisitor(this.programAST.sourceComments);
                result.getBody().accept(jsDocVisitor);

                return result;
            }
        }
    }

    public static Statement toStatement(AstNode node) {
        if (node instanceof Statement) {
            return (Statement)node;
        } else if (node instanceof FunctionExpression) {
            SourceRange loc = node.location;
            FunctionExpression func = (FunctionExpression) node;
            Identifier name = func.getName();
            func.name = null; // This way converting "function name() {}", to the equivalent (in top level functions) "var name = function () {}".
            return new VariableNode(loc, name, func);
        } else {
            throw new RuntimeException("Cannot make class into a statement for the top-program: " + node.getClass().getName());
        }
    }

    public class SourceLocation {
        private final int lineNumber;
        private final int columnNumber;
        private final String name2;

        public SourceLocation(int lineNumber, int columnNumber, String name2) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.name2 = name2;
        }
    }

    private class MutedErrorReporter extends com.google.javascript.jscomp.parsing.parser.util.ErrorReporter {
        @Override
        protected void reportError(SourcePosition sourcePosition, String s) { }

        @Override
        protected void reportWarning(SourcePosition sourcePosition, String s) { }
    }

    private static class FillFunctionsVariableDeclarations implements NodeTransverse<Void> {
        private final FunctionExpression function;

        public FillFunctionsVariableDeclarations(FunctionExpression function) {
            this.function = function;
            if (this.function.declarations != null) {
                throw new RuntimeException("Should not find variables in a function twice");
            }
            this.function.declarations = new HashMap<>();
        }

        @Override
        public Void visit(VariableNode variableNode) {
            boolean isTopLevelProgram = this.function.getName() != null && this.function.getName().getName().equals(":program");
            if (variableNode.getlValue() instanceof Identifier && !isTopLevelProgram) {
                Identifier id = (Identifier) variableNode.getlValue();
                this.function.declarations.put(id.getName(), id);
            }
            variableNode.getlValue().accept(this);
            variableNode.getInit().accept(this);
            return null;
        }

        @Override
        public Void visit(FunctionExpression function) {
            if (function == this.function) {
                function.getArguments().forEach(arg -> this.function.declarations.put(arg.getName(), arg));
                if (this.function.getName() != null) {
                    this.function.declarations.put(this.function.getName().getName(), this.function.getName());
                }
                return NodeTransverse.super.visit(function); // Actually visiting the children.
            } else {
                new FillFunctionsVariableDeclarations(function).visit(function);
                return null;
            }
        }
    }

    private static class FindVariableDeclarations implements NodeTransverse<Void> {
        private FunctionExpression function;
        private Map<String, Identifier> env;
        private Map<String, Identifier> globalEnv;

        public FindVariableDeclarations(FunctionExpression function, Map<String, Identifier> env, Map<String, Identifier> globalEnv) {
            this.function = function;
            this.env = new HashMap<>(env);
            this.globalEnv = globalEnv;

            this.env.putAll(this.function.declarations);
        }

        public FindVariableDeclarations(FunctionExpression result) {
            this(result, new HashMap<>(), new HashMap<>());
        }

        @Override
        public Void visit(Identifier identifier) {
            if (this.env.containsKey(identifier.getName())) {
                identifier.declaration = this.env.get(identifier.getName());
            } else if (this.globalEnv.containsKey(identifier.getName())) {
                identifier.declaration = identifier;
                identifier.isGlobal = true;
            } else {
                this.globalEnv.put(identifier.getName(), identifier);
                identifier.declaration = identifier;
                identifier.isGlobal = true;
            }
            return NodeTransverse.super.visit(identifier);
        }

        @Override
        public Void visit(FunctionExpression function) {
            if (this.function == function) {
                return NodeTransverse.super.visit(function);
            } else {
                new FindVariableDeclarations(function, env, globalEnv).visit(function);
                return null;
            }
        }

        @Override
        public Void visit(CatchStatement catchStatement) {
            String exceptionName = catchStatement.getException().getName();
            if (this.env.containsKey(exceptionName)) {
                NodeTransverse.super.visit(catchStatement);
            } else {
                Map<String, Identifier> newEnv = new HashMap<>(this.env);
                newEnv.put(exceptionName, catchStatement.getException());
                new FindVariableDeclarations(this.function, newEnv, this.globalEnv).visit(catchStatement);
            }
            return null;
        }
    }

    private static final class FindJSDocVisitor implements NodeTransverse<Void> {
        private final List<Comment> comments;

        FindJSDocVisitor(List<Comment> comments) {
            this.comments = comments.stream().filter(this::isJSDoc).collect(Collectors.toList());
        }

        private boolean isJSDoc(Comment comment) {
            return comment.isJsDoc() || (comment.value.startsWith("/*") && comment.value.contains("@"));
        }

        @Override
        public Void visit(FunctionExpression function) {
            List<Comment> docs = comments.stream().filter(comment -> comment.location.end.line == function.location.start.line - 1).collect(Collectors.toList());
            if (docs.size() == 1) {
                function.jsDoc = docs.iterator().next();
            }
            List<Comment> nestedComments = comments.stream().filter(comment -> comment.location.start.offset > function.location.start.offset && comment.location.end.offset < function.location.end.offset).collect(Collectors.toList());
            new FindMemberJSDocVisitor(function, nestedComments).visit(function);
            return NodeTransverse.super.visit(function);
        }
    }

    private static final class FindMemberJSDocVisitor implements NodeTransverse<Void> {
        private final FunctionExpression currentFunction;
        private final List<Comment> comments;

        private FindMemberJSDocVisitor(FunctionExpression currentFunction, List<Comment> comments) {
            this.currentFunction = currentFunction;
            this.comments = comments;
        }

        @Override
        public Void visit(FunctionExpression function) {
            if (function == currentFunction) {
                return NodeTransverse.super.visit(function);
            } else {
                return null; // No further.
            }
        }

        @Override
        public Void visit(BinaryExpression expression) {
            if (expression.getOperator() != Operator.EQUAL) {
                return NodeTransverse.super.visit(expression);
            }
            if (!(expression.getLhs() instanceof MemberExpression)) {
                return NodeTransverse.super.visit(expression);
            }

            MemberExpression lhs = (MemberExpression) expression.getLhs();

            if (!(lhs.getExpression() instanceof ThisExpression)) {
                return NodeTransverse.super.visit(expression);
            }

            int line = expression.location.start.line;

            List<Comment> memberComments = comments.stream().filter(comment -> comment.location.end.line == line - 1).collect(Collectors.toList());
            if (memberComments.size() == 1) {
                String name = lhs.getProperty();
                assert !currentFunction.memberJsDocs.containsKey(name);
                currentFunction.memberJsDocs.put(name, memberComments.iterator().next());
            }

            return NodeTransverse.super.visit(expression);
        }
    }
}
