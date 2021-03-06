package com.athaydes.osgiaas.javac.internal.compiler;

import com.athaydes.osgiaas.api.env.ClassLoaderContext;
import com.athaydes.osgiaas.javac.JavacService;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OsgiaasJavaCompilerService implements JavacService {

    private final Map<ClassLoader, OsgiaasJavaCompiler> compilerByClassLoader = new HashMap<>();

    @Override
    public <T> Optional<Class<T>> compileJavaClass( ClassLoaderContext classLoaderContext,
                                                    String qualifiedName,
                                                    String code,
                                                    PrintStream writer ) {
        OsgiaasJavaCompiler compiler = getOsgiaasJavaCompiler( classLoaderContext );
        return compiler.compile( qualifiedName, code, writer );
    }

    @Override
    public ClassLoaderContext getAugmentedClassLoaderContext( ClassLoaderContext classLoaderContext ) {
        OsgiaasJavaCompiler compiler = getOsgiaasJavaCompiler( classLoaderContext );
        return compiler.getClassLoaderContext();
    }

    private OsgiaasJavaCompiler getOsgiaasJavaCompiler( ClassLoaderContext classLoaderContext ) {
        return compilerByClassLoader
                .computeIfAbsent( classLoaderContext.getClassLoader(),
                        ( loader ) -> new OsgiaasJavaCompiler( classLoaderContext ) );
    }


}
