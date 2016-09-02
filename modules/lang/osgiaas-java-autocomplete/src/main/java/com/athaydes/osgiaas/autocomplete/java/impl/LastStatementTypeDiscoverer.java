package com.athaydes.osgiaas.autocomplete.java.impl;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.autocomplete.java.ResultType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/**
 * Method declaration visitor that can determine the type returned by the method's last statement.
 */
final class LastStatementTypeDiscoverer extends GenericVisitorAdapter<ResultType, MethodDeclaration> {

    private final TypeDiscoverer typeDiscoverer;

    LastStatementTypeDiscoverer( Iterable<String> importedClasses,
                                 @Nullable ClassLoaderContext classLoaderContext ) {
        this.typeDiscoverer = new TypeDiscoverer( importedClasses, classLoaderContext );
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
                return new ResultType( typeDiscoverer.classForName(
                        ( ( ObjectCreationExpr ) expr ).getType().getName() ), false );
            else if ( expr instanceof ArrayCreationExpr )
                return new ResultType( Array.class, false );
            else if ( expr.getClass().equals( StringLiteralExpr.class ) )
                return new ResultType( String.class, false );
            else if ( expr.getClass().equals( NameExpr.class ) ) {
                Map<String, Class<?>> typeByVariableName = typeDiscoverer.getTypesByVariableName( statements );
                String name = ( ( NameExpr ) expr ).getName();
                @Nullable Class<?> variableType = typeByVariableName.get( name );
                if ( variableType == null ) {
                    // attempt to return a class matching the apparent-variable name
                    return new ResultType( typeDiscoverer.classForName( name ), true );
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

}
