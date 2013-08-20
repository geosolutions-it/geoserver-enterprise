/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;
import it.geosolutions.geoserver.jms.impl.utils.JMSPropertyPlaceholderConfigurer;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * class to store and load configuration from global var or properties file
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ConnectionConfiguration implements JMSConfigurationExt {

    public static final String CONNECTION_KEY = "connection";

    public static final ConnectionConfigurationStatus DEFAULT_CONNECTION_STATUS = ConnectionConfigurationStatus.enabled;
    
    public static enum ConnectionConfigurationStatus {
        enabled,
        disabled
    }

    @Autowired
    // @Qualifier("JMSPropertyPlaceholderConfigurer")
    JMSPropertyPlaceholderConfigurer commonConfiguration;

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        String status = null;

        if (commonConfiguration != null) {
            status = commonConfiguration.getMergedProperties().getProperty(CONNECTION_KEY);
        }

        config.putConfiguration(CONNECTION_KEY, status != null ? status : DEFAULT_CONNECTION_STATUS.toString());
    }

    @Override
    public boolean checkForOverride(JMSConfiguration config) throws IOException {
        return config.checkForOverride(CONNECTION_KEY, DEFAULT_CONNECTION_STATUS);
    }

}
