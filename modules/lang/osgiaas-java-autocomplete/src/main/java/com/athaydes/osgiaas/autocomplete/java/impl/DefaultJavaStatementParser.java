package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.autocomplete.java.JavaStatementParser;
import com.athaydes.osgiaas.autocomplete.java.ResultType;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultJavaStatementParser implements JavaStatementParser {

    @Override
    public Map<String, ResultType> parseStatements( List<String> statements,
                                                    Collection<String> importedClasses )
            throws ParseException {
        return parseStatements( statements, importedClasses, null );
    }

    @Override
    public Map<String, ResultType> parseStatements( List<String> statements,
                                                    Collection<String> importedClasses,
                                                    @Nullable ClassLoaderContext classLoaderContext )
            throws ParseException {
        StringBuilder codeBuilder = new StringBuilder( "class X {" );
        codeBuilder.append( "void run() {" );
        for (String statement : statements) {
            codeBuilder.append( statement );
        }
        codeBuilder.append( "} }" );

        List<Statement> parsedStatements;

        try {
            CompilationUnit compilationUnit = JavaParser.parse(
                    new StringReader( codeBuilder.toString() ), false );

            MethodDeclaration methodDeclaration = ( MethodDeclaration ) compilationUnit
                    .getTypes().get( 0 )
                    .getMembers().get( 0 );

            parsedStatements = methodDeclaration.getBody().getStmts();
        } catch ( com.github.javaparser.ParseException e ) {
            throw new ParseException( e.getMessage(), 0 );
        }

        return resultTypes( new TypeDiscoverer( importedClasses, classLoaderContext )
                .getTypesByVariableName( parsedStatements ) );
    }

    private static Map<String, ResultType> resultTypes( Map<String, Class<?>> types ) {
        Map<String, ResultType> result = new HashMap<>( types.size() );

        for (Map.Entry<String, Class<?>> entry : types.entrySet()) {
            result.put( entry.getKey(), new ResultType( entry.getValue(), false ) );
        }

        return result;
    }

}
