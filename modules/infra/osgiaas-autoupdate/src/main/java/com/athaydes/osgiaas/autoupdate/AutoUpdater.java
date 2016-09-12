package com.athaydes.osgiaas.autoupdate;

/**
 * Auto-update Service.
 */
public interface AutoUpdater {

    /**
     * Register the bundle with the given ID for auto-update.
     *
     * @param bundleId bundle ID
     * @param options  options
     */
    void subscribeBundle( long bundleId, AutoUpdateOptions options );

    /**
     * Register the bundle with the given ID for auto-update.
     *
     * @param bundleSymbolicName symbolic name of the bundle to subscribe
     * @param options            options
     */
    void subscribeBundle( String bundleSymbolicName, AutoUpdateOptions options );

    /**
     * Register all bundles for auto-update.
     *
     * @param options    options
     * @param exclusions bundle IDs or regular expressions to match against Bundle-SymbolicName
     *                   that should be excluded from auto-update.
     *                   <p>
     *                   Each String should be parsed using the following algorithm:
     *                   <ul>
     *                   <li>if the String can be parsed as a long, use it to represent a bundle ID</li>
     *                   <li>otherwise, use the String as a regular expression</li>
     *                   <li>whitespaces in a String should be interpreted as a separator, so that
     *                   it is possible to provide a single String to represent several bundle IDs
     *                   or regular expressions. For example, the String '10 osgi.*' is equivalent to
     *                   passing the two Strings '10' and 'osgi.*'. This allows configuration
     *                   provided via String properties to be parsed unambiguously.</li>
     *                   </ul>
     */
    void subscribeAllBundles( AutoUpdateOptions options, String... exclusions );

}
