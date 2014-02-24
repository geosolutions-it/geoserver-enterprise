/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

import java.io.IOException;

/**
 * 
 * class to store and load configuration from global var or properties file
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ConnectionConfiguration implements JMSConfigurationExt {

    public static final String CONNECTION_KEY = "connection";

    public static final ConnectionConfigurationStatus DEFAULT_CONNECTION_STATUS = ConnectionConfigurationStatus.disabled;

    public static enum ConnectionConfigurationStatus {
        enabled, disabled
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        String status = null;

        config.putConfiguration(CONNECTION_KEY,
                status != null ? status : DEFAULT_CONNECTION_STATUS.toString());
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(CONNECTION_KEY, DEFAULT_CONNECTION_STATUS);
    }

}
