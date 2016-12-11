package com.athaydes.osgiaas.cli.ivy;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Thin wrapper around {@link Ivy} that makes it easy to resolve a dependency.
 */
public class IvyResolver {

    private static final Filter JARS_ONLY_FILTER = ( obj ) -> ( ( obj instanceof Artifact ) &&
            "jar".equals( ( ( Artifact ) obj ).getType() ) );

    private static final Filter NO_FILTER = ( obj ) -> true;

    private final Ivy ivy;

    private boolean includeTransitive = true;
    private Filter artifactFilter = JARS_ONLY_FILTER;

    public IvyResolver() {
        this( Ivy.newInstance() );
    }

    public IvyResolver( Ivy ivy ) {
        this.ivy = ivy;
    }

    public IvyResolver includeTransitiveDependencies( boolean include ) {
        this.includeTransitive = include;
        return this;
    }

    public IvyResolver downloadJarOnly( boolean downloadJarOnly ) {
        this.artifactFilter = downloadJarOnly ? JARS_ONLY_FILTER : NO_FILTER;
        return this;
    }

    public ResolveReport resolve( String group, String module, String version ) {
        DefaultModuleDescriptor moduleDescriptor =
                DefaultModuleDescriptor.newDefaultInstance( ModuleRevisionId.newInstance( group,
                        module + "-caller", "working" ) );

        moduleDescriptor.addConfiguration( new Configuration( "default" ) );

        DefaultDependencyDescriptor dependencyDescriptor = new DefaultDependencyDescriptor( moduleDescriptor,
                ModuleRevisionId.newInstance( group, module, version ), false, false, includeTransitive );

        moduleDescriptor.addDependency( dependencyDescriptor );

        try {
            File ivyfile = File.createTempFile( "ivy", ".xml" );
            ivyfile.deleteOnExit();

            XmlModuleDescriptorWriter.write( moduleDescriptor, ivyfile );

            return ivy.resolve( ivyfile.toURI().toURL(), new ResolveOptions()
                    .setConfs( new String[]{ "default" } )
                    .setArtifactFilter( artifactFilter )
                    .setTransitive( includeTransitive ) );
        } catch ( ParseException | IOException e ) {
            throw new RuntimeException( e );
        }
    }


}
