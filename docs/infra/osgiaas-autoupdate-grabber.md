# OSGiaaS Autoupdate Grabber Module

> This module is still in an alpha version

This module is an implementation of the [osgiaas-autoupdate](osgiaas-autoupdate.md) module that uses the
[osgiaas-grab](osgiaas-grab) module to check and download newer versions of installed bundles.

When the `osgiaas-autoupdate` bundle requests that a particular bundle be auto-updated, this bundle will look for a
newer version of the particular bundle in the configured repositories.

If a newer version is found and the bundle has not been updated within the configured minimum autoupdate time,
the new version is immediately downloaded and the bundle updated.

## Subscribing a bundle for auto-update

All bundles whose manifests contain an entry called `Osgiaas-Bundle-Coordinates` will be auto-updated.

The value of this entry should be the bundle coordinates in the Maven repository (as defined in the osgiaas-grab module).

For example:

```properties
Osgiaas-Bundle-Coordinates: com.athaydes.osgiaas:osgiaas-cli-api:1.0
```
