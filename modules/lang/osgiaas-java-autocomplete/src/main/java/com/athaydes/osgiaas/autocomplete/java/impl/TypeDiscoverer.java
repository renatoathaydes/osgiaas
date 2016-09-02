package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple helper class that can discover the types of variables declared in a List of Statements.
 */
final class TypeDiscoverer {

    private final Iterable<String> importedClasses;

    @Nullable
    private final ClassLoaderContext classLoaderContext;

    TypeDiscoverer( Iterable<String> importedClasses,
                    @Nullable ClassLoaderContext classLoaderContext ) {
        this.importedClasses = importedClasses;
        this.classLoaderContext = classLoaderContext;
    }

    Map<String, Class<?>> getTypesByVariableName( List<Statement> statements ) {
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
    Class<?> typeOf( Type type ) {
        if ( type instanceof ReferenceType ) {
            ReferenceType referenceType = ( ReferenceType ) type;
            if ( referenceType.getArrayCount() > 0 ) {
                return Array.class;
            }
            // unwrap the reference type and try again
            return typeOf( referenceType.getType() );
        }

        if ( type instanceof PrimitiveType ) {
            String typeName = ( ( PrimitiveType ) type ).getType().name();
            return primitiveType( typeName );
        } else if ( type instanceof ClassOrInterfaceType ) try {
            return classForName( ( ( ClassOrInterfaceType ) type ).getName() );
        } catch ( ClassNotFoundException e ) {
            // class not found, ignore
        }

        return null;
    }

    private Class<?> primitiveType( String typeName ) {
        switch ( typeName ) {
            case "Int":
                return int.class;
            case "Long":
                return long.class;
            case "Float":
                return float.class;
            case "Byte":
                return byte.class;
            case "Short":
                return short.class;
            case "Boolean":
                return boolean.class;
            case "Char":
                return char.class;
            default:
                throw new RuntimeException( "Unknown primitive type: " + typeName );
        }

    }

    Class<?> classForName( String name ) throws ClassNotFoundException {
        for (String imp : importedClasses) {
            @Nullable String potentialClassName = imp.endsWith( "." + name ) ?
                    imp :
                    imp.endsWith( ".*" ) ?
                            imp.substring( 0, imp.length() - 1 ) + name :
                            null;
            if ( potentialClassName != null ) {
                try {
                    return findClass( potentialClassName );
                } catch ( ClassNotFoundException ignore ) {
                    // try something else
                }
            }
        }

        // try just the simple name without a package name
        try {
            return findClass( name );
        } catch ( ClassNotFoundException ignore ) {
            // didn't work
        }

        // last guess
        return findClass( "java.lang." + name );
    }

    private Class<?> findClass( String className ) throws ClassNotFoundException {
        if ( classLoaderContext == null ) {
            return Class.forName( className );
        } else {
            return classLoaderContext.getClassLoader().loadClass( className );
        }
    }

}
