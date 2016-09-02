package com.athaydes.osgiaas.autocomplete.java;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.autocomplete.java.impl.DefaultJavaStatementParser;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A parser of Java statements which can be used to discover variable declarations.
 */
public interface JavaStatementParser {

    /**
     * Parse the given Java statements, returning a Map containing the name of each declared variable associated
     * with its type.
     *
     * @param statements      to parse
     * @param importedClasses all imported classes that should be visible by each statement
     * @return the name and type of each variable declaration
     * @throws ParseException if any statement has invalid Java syntax or is not a valid Java statement.
     */
    Map<String, ResultType> parseStatements( List<String> statements, Collection<String> importedClasses )
            throws ParseException;

    /**
     * Parse the given Java statements, returning a Map containing the name of each declared variable associated
     * with its type.
     *
     * @param statements         to parse
     * @param importedClasses    all imported classes that should be visible by each statement
     * @param classLoaderContext classLoader context to use when trying to locate classes
     * @return the name and type of each variable declaration
     * @throws ParseException if any statement has invalid Java syntax or is not a valid Java statement.
     */
    Map<String, ResultType> parseStatements( List<String> statements,
                                             Collection<String> importedClasses,
                                             ClassLoaderContext classLoaderContext )
            throws ParseException;

    /**
     * @return a default Java parser.
     */
    static JavaStatementParser getDefaultParser() {
        return new DefaultJavaStatementParser();
    }

}
