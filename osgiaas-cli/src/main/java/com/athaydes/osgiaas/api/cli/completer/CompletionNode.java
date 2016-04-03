package com.athaydes.osgiaas.api.cli.completer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface CompletionNode {
    String name();

    List<CompletionNode> children();

    static CompletionNodeBuilder nodeFor( String name ) {
        return new CompletionNodeBuilder( name );
    }

    class CompletionNodeBuilder {
        private final List<CompletionNode> children = new ArrayList<>( 4 );
        private final String name;

        private CompletionNodeBuilder( String name ) {
            this.name = name;
        }

        public CompletionNodeBuilder withChild( CompletionNode child ) {
            children.add( child );
            return this;
        }

        public CompletionNodeBuilder withChildren( Collection<? extends CompletionNode> children ) {
            this.children.addAll( children );
            return this;
        }

        public CompletionNode build() {
            if ( name.isEmpty() ) {
                throw new IllegalArgumentException( "Name cannot be empty" );
            }
            return new CompletionNode() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public List<CompletionNode> children() {
                    return children;
                }
            };
        }
    }
}