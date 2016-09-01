package com.athaydes.osgiaas.autocomplete.java.impl;

/**
 * Result of discovering the return type of a method.
 * <p>
 * Besides the type returned by the method, the result includes also a flag indicating if access to the type is static
 * (ie. whether or not the type is an instance of the type, or the type itself).
 */
final class ResultType {
    final Class<?> type;
    final boolean isStatic;
    static final ResultType VOID = new ResultType( Void.TYPE, false );

    ResultType( Class<?> type, boolean isStatic ) {
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
