package com.athaydes.osgiaas.autocomplete.java.impl;

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
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Method declaration visitor that can determine the type returned by the method's last statement.
 */
final class LastStatementTypeDiscoverer extends GenericVisitorAdapter<ResultType, MethodDeclaration> {

    private final Iterable<String> importedClasses;

    LastStatementTypeDiscoverer( Iterable<String> importedClasses ) {
        this.importedClasses = importedClasses;
    }

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
        for (String imp : importedClasses) {
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
