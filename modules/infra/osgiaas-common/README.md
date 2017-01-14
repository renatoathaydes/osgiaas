# OSGiaaS Common Module

Small, common functionality module for all OSGiaaS implementation bundles.

Depending on this bundle is not a requirement for any module. Its only purpose is to be the module where the most
basic, generic functionality and interfaces from core bundles can be placed, thus avoiding complicated interdependencies
between modules where only very basic functionality should be shared.

The following packages are exported by the OSGiaaS Common bundle:

* `com.athaydes.osgiaas.api.ansi` - helper classes to work with
  [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code)
* `com.athaydes.osgiaas.api.env` - runtime environment interfaces
* `com.athaydes.osgiaas.api.service` - helper classes to make it easier to work with OSGi services
* `com.athaydes.osgiaas.api.stream` - helper classes to make it easier to work streams of data

General documentation about the OSGIaaS Project can be found in the [docs](../../../docs) directory.
