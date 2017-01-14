# OSGiaaS Autoupdate Module

> Status: alpha

This module defines a simple auto-update API for OSGi bundles, as well as a means for the auto-update functionality
to be configured.

**For auto-update functionality to actually work, an implementation of the API is required.**

The [osgiaas-autoupdate-grabber](osgiaas-autoupdate-grabber.md) module is the default implementation of the API,
and must be installed for auto-update to work.

## Configuring the auto-update service

The auto-update service can be configured in two different ways:

### OSGi Config Admin

The `osgiaas-autoupdate` bundle may be configured with the
[Osgi Config Admin](http://felix.apache.org/documentation/subprojects/apache-felix-config-admin.html) service.

This is the preferred way to configure this module.

### System properties

If the Config Admin is not used, this module will check the system properties.

## Configuration properties

Whichever way you use to provide configuration, the following properties may be provided:

* `osgiaas.autoupdate.frequency` - update frequency in seconds
* `osgiaas.autoupdate.repositories` - list of repositories to use to search for artifacts
* `osgiaas.autoupdate.bundle_excludes` - list of bundles (symbolic names) to exclude from auto-update
