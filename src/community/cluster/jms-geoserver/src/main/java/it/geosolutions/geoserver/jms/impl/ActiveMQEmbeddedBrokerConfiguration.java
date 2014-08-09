/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;

import java.io.IOException;

/**
 * 
 * class to store and load configuration
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ActiveMQEmbeddedBrokerConfiguration implements JMSConfigurationExt {

    public final static String BROKER_URL_KEY = "xbeanURL";

    public final static String DEFAULT_BROKER_URL = "./broker.xml";

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }

}
