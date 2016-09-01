package com.athaydes.osgiaas.autocomplete.java;

/**
 * Result of discovering the return type of a method.
 * <p>
 * Besides the type returned by the method, the result includes also a flag indicating if access to the type is static
 * (ie. whether or not the type is an instance of the type, or the type itself).
 */
public final class ResultType {

    public static final ResultType VOID = new ResultType( Void.TYPE, false );

    private final Class<?> type;
    private final boolean isStatic;

    public ResultType( Class<?> type, boolean isStatic ) {
        this.type = type;
        this.isStatic = isStatic;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other ) return true;
        if ( other == null || getClass() != other.getClass() ) return false;

        ResultType that = ( ResultType ) other;

        return isStatic == that.isStatic && type.equals( that.type );
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + ( isStatic ? 1 : 0 );
        return result;
    }

    @Override
    public String toString() {
        return "ResultType{" +
                "type=" + type +
                ", isStatic=" + isStatic +
                '}';
    }
}
