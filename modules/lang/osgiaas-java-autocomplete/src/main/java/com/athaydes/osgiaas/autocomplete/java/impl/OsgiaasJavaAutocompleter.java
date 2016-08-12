package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.autocomplete.Autocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleteContext;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleter;
import com.athaydes.osgiaas.autocomplete.java.JavaAutocompleterResult;
import com.athaydes.osgiaas.cli.CommandHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            "final", "finally", "float", "for", "goto", "if", "implements",
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
        LinkedList<String> codeParts = new LinkedList<>();
        CommandHelper.breakupArguments( codeFragment, codeParts::add, OPTIONS );

        if ( codeParts.size() < 2 ) {
            return new JavaAutocompleterResult( textCompleter.completionsFor(
                    codeFragment,
                    topLevelCompletions( bindings.keySet() ) ), 0 );
        } else {
            LastTypeAndTextToComplete lttc = lastTypeAndTextToComplete( codeParts, bindings );
            List<String> options = optionsFor( lttc.lastType );
            String text = lttc.textToComplete;
            int index = codeFragment.length() - text.length();
            return new JavaAutocompleterResult(
                    textCompleter.completionsFor( text, options ), index );
        }
    }

    private List<String> optionsFor( Class<?> type ) {
        Stream<String> methods = Stream.of( type.getMethods() )
                .map( this::completionFor );

        Stream<String> fields = Stream.of( type.getFields() )
                .map( Field::getName );

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

        Class<?> topLevelType = topLevelType( codeParts.removeFirst(), bindings );

        Class<?> lastType = findLastType( codeParts, topLevelType );
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

    protected Class<?> findTypeOfFirstPart( String code ) {
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
        } catch ( ParseException e ) {
            e.printStackTrace();
            return Void.class;
        }
    }

    protected Class<?> topLevelType( String text, Map<String, Object> bindings ) {
        @Nullable Object object = bindings.get( text );
        if ( object != null ) {
            return object.getClass();
        }

        return findTypeOfFirstPart( text );
    }

    private Class<?> findLastType( LinkedList<String> codeParts, Class<?> type ) {
        while ( codeParts.size() > 1 ) {
            String part = codeParts.removeFirst();
            int openBracketIndex = part.indexOf( '(' );
            if ( openBracketIndex > 0 ) {
                String methodName = part.substring( 0, openBracketIndex );
                Optional<Method> firstMethod = Stream.of( type.getMethods() )
                        .filter( it -> it.getName().equals( methodName ) )
                        .findFirst();
                if ( firstMethod.isPresent() ) {
                    type = firstMethod.get().getReturnType();
                } else {
                    // no completion possible
                    type = Void.class;
                    break;
                }
            } else {
                Optional<Field> field = Stream.of( type.getFields() )
                        .filter( it -> it.getName().equals( part ) )
                        .findFirst();
                if ( field.isPresent() ) {
                    type = field.get().getType();
                } else {
                    // no completion possible
                    type = Void.class;
                    break;
                }
            }
        }
        return type;
    }

    protected String completionFor( Method method ) {
        if ( method.getParameterCount() > 0 ) {
            return method.getName() + "(";
        } else {
            return method.getName() + "()";
        }
    }

    protected static class LastTypeAndTextToComplete {
        final Class<?> lastType;
        final String textToComplete;

        public LastTypeAndTextToComplete( Class<?> lastType, String textToComplete ) {
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

    private class LastStatementTypeDiscoverer extends GenericVisitorAdapter<Class<?>, MethodDeclaration> {

        Class<?> discover( CompilationUnit cu ) {
            return visit( cu, null );
        }

        @Override
        public Class<?> visit( MethodDeclaration declaration, MethodDeclaration arg ) {
            List<Statement> statements = declaration.getBody().getStmts();
            Statement lastStatement = statements.get( statements.size() - 1 );

            if ( lastStatement instanceof ReturnStmt ) try {
                Expression expr = ( ( ReturnStmt ) lastStatement ).getExpr();
                if ( expr instanceof ClassExpr )
                    return Class.class;
                else if ( expr instanceof ObjectCreationExpr )
                    return classForName( ( ( ObjectCreationExpr ) expr ).getType().getName() );
                else if ( expr.getClass().equals( StringLiteralExpr.class ) )
                    return String.class;
                else
                    return Void.class;
            } catch ( ClassNotFoundException ignore ) {
                // class does not exist
            }

            return Void.class;
        }

        private Class<?> classForName( String name ) throws ClassNotFoundException {
            Optional<String> importedType = context.getImports().stream()
                    .filter( type -> type.endsWith( "." + name ) )
                    .findFirst();
            if ( importedType.isPresent() ) {
                try {
                    return Class.forName( importedType.get() );
                } catch ( ClassNotFoundException ignore ) {
                    // try something else
                }
            }

            try {
                return Class.forName( name );
            } catch ( ClassNotFoundException ignore ) {
                // try something else
            }

            // last guess
            return Class.forName( "java.lang." + name );
        }

    }

}

