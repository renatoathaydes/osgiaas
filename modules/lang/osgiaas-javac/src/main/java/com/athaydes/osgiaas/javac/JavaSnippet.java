package com.athaydes.osgiaas.javac;

import java.util.Collection;

import static java.util.Collections.emptyList;

/**
 * Representation of a Java code snippet.
 * <p>
 * Instances of types implementing this interface can be used to run
 * Java code using the {@link JavacService} implementations.
 * <p>
 * A trivial implementation of this class is provided with the nested {@link Builder} class.
 */
public interface JavaSnippet {

    /**
     * @return the executable code. This is the code that will go in the body of the runnable method.
     */
    String getExecutableCode();

    /**
     * @return class imports. Eg. ['java.util.*', 'com.acme']
     */
    Collection<String> getImports();

    class Builder implements JavaSnippet {

        private String code = "";
        private Collection<String> imports = emptyList();

        private Builder() {
        }

        public static Builder withCode( String code ) {
            Builder b = new Builder();
            b.code = code;
            return b;
        }

        public Builder withImports( Collection<String> imports ) {
            this.imports = imports;
            return this;
        }

        @Override
        public String getExecutableCode() {
            return code;
        }

        @Override
        public Collection<String> getImports() {
            return imports;
        }

    }

}