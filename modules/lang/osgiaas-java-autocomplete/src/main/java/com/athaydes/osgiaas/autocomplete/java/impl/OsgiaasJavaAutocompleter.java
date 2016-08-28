package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.autocomplete.Autocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleterResult;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.athaydes.osgiaas.cli.CommandHelper.CommandBreakupOptions;
import static com.athaydes.osgiaas.cli.CommandHelper.DOUBLE_QUOTE_CODE;
import static com.athaydes.osgiaas.cli.CommandHelper.SINGLE_QUOTE_CODE;

public class OsgiaasJavaAutocompleter implements JavaAutocompleter {

    private static final CommandBreakupOptions OPTIONS = CommandBreakupOptions.create()
            .includeQuotes( true )
            .separatorCode( '.' )
            .quoteCodes( DOUBLE_QUOTE_CODE, SINGLE_QUOTE_CODE );

    public static final List<String> JAVA_KEYWORDS = Arrays.asList(
            "abstract", "assert", "boolean",
            "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "false",
            "final", "finally", "float", "for",
            // reserved keyword, but not used
            // "goto",
            "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"
    );

    private final Autocompleter textCompleter;
    private final JavaAutocompleteContext context;

    public OsgiaasJavaAutocompleter( Autocompleter textCompleter,
                                     JavaAutocompleteContext context ) {
        this.textCompleter = textCompleter;
        this.context = context;
    }

    @Override
    public JavaAutocompleterResult completionsFor( String codeFragment, Map<String, Object> bindings ) {
        int startIndex = indexToStartCompletion( codeFragment );
        String toComplete = codeFragment.substring( startIndex );
        LinkedList<String> codeParts = new LinkedList<>();
        CommandHelper.breakupArguments( toComplete, codeParts::add, OPTIONS );

        // ensure an empty last part in case the code fragment ends with a dot
        if ( codeFragment.trim().endsWith( "." ) ) {
            codeParts.add( "" );
        }

        if ( codeParts.size() < 2 ) {
            List<String> completions = textCompleter.completionsFor(
                    toComplete,
                    topLevelCompletions( bindings.keySet() ) );

            return new JavaAutocompleterResult( completions, startIndex );
        } else {
            LastTypeAndTextToComplete lttc = lastTypeAndTextToComplete( codeParts, bindings );
            List<String> options = optionsFor( lttc.lastType );
            String text = lttc.textToComplete;
            int index = toComplete.length() - text.length();

            return new JavaAutocompleterResult(
                    textCompleter.completionsFor( text, options ), startIndex + index );
        }
    }

    /**
     * @param codeFragment original code fragment
     * @return last index, excepting whitespaces, right after a code delimiter.
     * If the 'new' operator is found right before the chosen index, it is included as well.
     */
    int indexToStartCompletion( String codeFragment ) {
        IntFunction<Integer> maybeIndexOfPreviousNew = ( index ) -> {
            for (int i = index - 1; i > 0; i--) {
                char c = codeFragment.charAt( i );
                if ( c == ' ' || c == '\n' ) {
                    continue;
                }
                // found non-whitespace previous to index, check if it's the 'new' keyword
                if ( i > 1 && codeFragment.substring( i - 2, i + 1 ).equals( "new" ) ) {
                    return i - 2;
                } else {
                    break;
                }
            }
            return index;
        };

        Set<Character> codeDelimiters = new HashSet<>( Arrays.asList( ';', '{', '}', ' ', '\n', '\t' ) );
        boolean ignoringWhitespace = true;
        for (int i = codeFragment.length() - 1; i > 0; i--) {
            char c = codeFragment.charAt( i );
            if ( ignoringWhitespace && c == ' ' ) continue;
            ignoringWhitespace = false;
            if ( codeDelimiters.contains( c ) ) {
                return maybeIndexOfPreviousNew.apply( i + 1 );
            }
        }

        return 0;
    }

    private List<String> optionsFor( ResultType resultType ) {
        Class<?> type = resultType.type;

        Stream<String> methods = filterMembers(
                Stream.of( type.getMethods() ), resultType.isStatic )
                .map( this::completionFor );

        Stream<String> fields;

        if ( type == Array.class && !resultType.isStatic ) {
            // Java arrays have only one synthetic field: length
            fields = Stream.of( "length" );
        } else {
            fields = filterMembers(
                    Stream.of( type.getFields() ), resultType.isStatic )
                    .map( Field::getName );
        }

        return Stream.concat( methods, fields )
                .sorted()
                .collect( Collectors.toList() );
    }

    protected List<String> topLevelCompletions( Collection<String> bindingNames ) {
        List<String> result = new ArrayList<>( JAVA_KEYWORDS.size() + bindingNames.size() );
        result.addAll( JAVA_KEYWORDS );
        result.addAll( bindingNames );
        return result;
    }

    protected LastTypeAndTextToComplete lastTypeAndTextToComplete(
            LinkedList<String> codeParts, Map<String, Object> bindings ) {
        assert codeParts.size() >= 2;

        String firstPart = codeParts.removeFirst();

        // special case for class expressions
        if ( codeParts.getFirst().equals( "class" ) ) {
            firstPart += "." + codeParts.removeFirst();
        }

        ResultType topLevelType = topLevelType( firstPart, bindings );
        ResultType lastType = findLastType( codeParts, topLevelType );

        if ( lastType.type.isArray() ) {
            lastType = new ResultType( Array.class, false );
        }

        String lastPart = codeParts.getLast();

        return new LastTypeAndTextToComplete( lastType, lastPart );
    }

    private static String generateClass( JavaAutocompleteContext context, String code ) {
        StringBuilder builder = new StringBuilder();

        for (String importing : context.getImports()) {
            builder.append( "import " ).append( importing ).append( ";" );
        }
        builder.append( "class A { public Object main() {" )
                .append( context.getMethodBody( code ) )
                .append( "}}" );

        return builder.toString();
    }

    protected ResultType findTypeOfFirstPart( String code ) {
        int lastDot = code.lastIndexOf( '.' );
        if ( lastDot > 0 ) {
            String lastPart = code.substring( lastDot, code.length() );
            if ( !lastPart.equals( ".class" ) ) {
                code = code.substring( 0, lastDot );
            }
        }
        code = code.trim();
        if ( !code.startsWith( "return " ) )
            code = "return " + code;
        if ( !code.endsWith( ";" ) )
            code += ";";

        String classCode = generateClass( context, code );

        try {
            CompilationUnit cu = JavaParser.parse( new StringReader( classCode ), false );
            return new LastStatementTypeDiscoverer().discover( cu );
        } catch ( Throwable e ) {
            return ResultType.VOID;
        }
    }

    protected ResultType topLevelType( String text, Map<String, Object> bindings ) {
        @Nullable Object object = bindings.get( text );
        if ( object != null ) {
            return new ResultType( object.getClass(), false );
        }

        return findTypeOfFirstPart( text );
    }

    private static <M extends Member> Stream<M> filterMembers( Stream<M> members, boolean isStatic ) {
        Predicate<Member> keepStaticMembersOrNot = isStatic ?
                ( member ) -> Modifier.isStatic( member.getModifiers() ) :
                ( member ) -> !Modifier.isStatic( member.getModifiers() );

        return members.filter( keepStaticMembersOrNot );
    }

    private ResultType findLastType( LinkedList<String> codeParts, ResultType type ) {
        ResultType current = type;

        while ( codeParts.size() > 1 ) {
            String part = codeParts.removeFirst();

            int openBracketIndex = part.indexOf( '(' );

            if ( openBracketIndex > 0 ) {
                String methodName = part.substring( 0, openBracketIndex );
                Optional<Method> firstMethod = filterMembers(
                        Stream.of( current.type.getMethods() ), current.isStatic )
                        .filter( it -> it.getName().equals( methodName ) )
                        .findFirst();

                if ( firstMethod.isPresent() ) {
                    Class<?> fieldType = firstMethod.get().getReturnType();
                    boolean isStatic = fieldType.equals( Class.class );
                    current = new ResultType( fieldType, isStatic );
                } else {
                    // no completion possible
                    current = ResultType.VOID;
                    break;
                }
            } else {
                Optional<Field> field = filterMembers(
                        Stream.of( type.type.getFields() ), current.isStatic )
                        .filter( it -> it.getName().equals( part ) )
                        .findFirst();

                if ( field.isPresent() ) {
                    current = new ResultType( field.get().getType(), false );
                } else {
                    // no completion possible
                    current = ResultType.VOID;
                    break;
                }
            }
        }

        return current;
    }

    protected String completionFor( Method method ) {
        if ( method.getParameterCount() > 0 ) {
            return method.getName() + "(";
        } else {
            return method.getName() + "()";
        }
    }

    protected static class LastTypeAndTextToComplete {
        final ResultType lastType;
        final String textToComplete;

        public LastTypeAndTextToComplete( ResultType lastType,
                                          String textToComplete ) {
            this.lastType = lastType;
            this.textToComplete = textToComplete;
        }

        @Override
        public String toString() {
            return "LastTypeAndTextToComplete{" +
                    "lastType=" + lastType +
                    ", textToComplete='" + textToComplete + '\'' +
                    '}';
        }
    }

    protected static class ResultType {
        final Class<?> type;
        final boolean isStatic;
        static final ResultType VOID = new ResultType( Void.TYPE, false );

        protected ResultType( Class<?> type, boolean isStatic ) {
            this.type = type;
            this.isStatic = isStatic;
        }

        @Override
        public String toString() {
            return "ResultType{" +
                    "type=" + type +
                    ", isStatic=" + isStatic +
                    '}';
        }
    }

    private class LastStatementTypeDiscoverer extends GenericVisitorAdapter<ResultType, MethodDeclaration> {

        ResultType discover( CompilationUnit cu ) {
            return visit( cu, null );
        }

        @Override
        public ResultType visit( MethodDeclaration declaration, MethodDeclaration arg ) {
            List<Statement> statements = declaration.getBody().getStmts();

            Statement lastStatement = statements.get( statements.size() - 1 );

            if ( lastStatement instanceof ReturnStmt ) try {
                Expression expr = ( ( ReturnStmt ) lastStatement ).getExpr();
                if ( expr instanceof ClassExpr )
                    // we could return the correct class type here, but that would cause misleading auto-completions
                    // because the runtime of the expression is a Class without with the type parameter erased
                    return new ResultType( Class.class, false );
                else if ( expr instanceof ObjectCreationExpr )
                    return new ResultType( classForName( ( ( ObjectCreationExpr ) expr ).getType().getName() ), false );
                else if ( expr instanceof ArrayCreationExpr )
                    return new ResultType( Array.class, false );
                else if ( expr.getClass().equals( StringLiteralExpr.class ) )
                    return new ResultType( String.class, false );
                else if ( expr.getClass().equals( NameExpr.class ) ) {
                    Map<String, Class<?>> typeByVariableName = getTypesByVariableName( statements );
                    String name = ( ( NameExpr ) expr ).getName();
                    @Nullable Class<?> variableType = typeByVariableName.get( name );
                    if ( variableType == null ) {
                        // attempt to return a class matching the apparent-variable name
                        return new ResultType( classForName( name ), true );
                    } else {
                        return new ResultType( variableType, false );
                    }
                } else
                    return ResultType.VOID;
            } catch ( ClassNotFoundException ignore ) {
                // class does not exist
            }

            return ResultType.VOID;
        }

        private Map<String, Class<?>> getTypesByVariableName( List<Statement> statements ) {
            Map<String, Class<?>> typeByVariableName = new HashMap<>();

            for (Statement statement : statements) {
                if ( statement instanceof ExpressionStmt ) {
                    Expression expression = ( ( ExpressionStmt ) statement ).getExpression();
                    if ( expression instanceof VariableDeclarationExpr ) {
                        VariableDeclarationExpr varExpression = ( VariableDeclarationExpr ) expression;
                        @Nullable Class<?> type = typeOf( varExpression.getType() );
                        if ( type != null ) {
                            for (VariableDeclarator var : varExpression.getVars()) {
                                typeByVariableName.put( var.getId().getName(), type );
                            }
                        }
                    }
                }
            }

            return typeByVariableName;
        }

        @Nullable
        private Class<?> typeOf( Type type ) {
            if ( type instanceof ReferenceType ) {
                ReferenceType referenceType = ( ReferenceType ) type;
                if ( referenceType.getArrayCount() > 0 ) {
                    return Array.class;
                }
                // unwrap the reference type and try again
                return typeOf( referenceType.getType() );
            }
            if ( type instanceof ClassOrInterfaceType ) {
                try {
                    return classForName( ( ( ClassOrInterfaceType ) type ).getName() );
                } catch ( ClassNotFoundException e ) {
                    // class not found, ignore
                }
            }
            return null;
        }

        private Class<?> classForName( String name ) throws ClassNotFoundException {
            for (String imp : context.getImports()) {
                @Nullable String potentialClassName = imp.endsWith( "." + name ) ?
                        imp :
                        imp.endsWith( ".*" ) ?
                                imp.substring( 0, imp.length() - 1 ) + name :
                                null;
                if ( potentialClassName != null ) {
                    try {
                        return Class.forName( potentialClassName );
                    } catch ( ClassNotFoundException ignore ) {
                        // try something else
                    }
                }
            }

            // last guess
            return Class.forName( "java.lang." + name );
        }

    }

}

