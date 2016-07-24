package com.athaydes.osgiaas.autoupdate.config;

public class ConfigurationException extends RuntimeException {

    private final String property;

    public ConfigurationException( String property, String message, Throwable cause ) {
        super( message, cause );
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
