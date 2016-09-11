# OSGiaaS SLF4J-Impl Module

Implementation of [SLF4J API](http://www.slf4j.org/) that forwards logs to the
[OSGi LogService](https://osgi.org/javadoc/r6/cmpn/index.html).

To use this bundle, simply install and start it in any OSGi container where a OSGi LogService is available.

Any bundles (including wrapped jars) which use the SFL4J API for logging will resolve without errors, and all their
logging will be available for consumption by OSGi `LogListener` instances.
