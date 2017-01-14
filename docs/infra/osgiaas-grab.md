# OSGiaaS Grab Module

> Status: beta

Module implementing a service to download (or grab) dependencies from Maven repositories such as
[JCenter](https://jcenter.bintray.com/) (enabled by default) and [Maven Central](https://repo1.maven.org/maven2).

Groovy's [Grapes](http://docs.groovy-lang.org/latest/html/documentation/grape.html) dependency manager
(which is, in turn, based on [Ivy](http://ant.apache.org/ivy/)) is used to implement this service.

## Using Grab in non-OSGi applications

The main implementation service is provided by the `com.athaydes.osgiaas.grab.Grabber` class.

The following snippet illustrates how Java code can be used to grab a Maven dependency from Maven Central:

```java
Map<String, String> repositories = new HashMap<>( 1 );
repositories.put( "Maven Central", "https://repo1.maven.org/maven2" );

Grabber grabber = new Grabber( repositories );

try {
    GrabResult result = grabber.grab( artifact );
    System.out.println( "Artifact downloaded to " + result.getGrapeFile() );
} catch ( GrabException e ) {
    System.err.printf( "Problem grabbing artifact %s: %s", artifact, e.getMessage() );
}
```

Full example at [GrabberSample.java](../../modules/infra/osgiaas-grab/src/test/java/com/athaydes/osgiaas/grab/sample/GrabberSample.java).

## Using Grab as an OSGi Service

The `com.athaydes.osgiaas.grab.Grabber` service is exported via
[OSGi Declarative Services](http://enroute.osgi.org/doc/217-ds.html),
so for the service to be accessible (with or without DS), a SCR (Service Component Runtime) must be installed in
the OSGi container.

Once the service is imported, usage is as for non-OSGi applications.
