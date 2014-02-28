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
final public class BrokerConfiguration implements JMSConfigurationExt {

	public static final String BROKER_URL_KEY = "brokerURL";

	public static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

	@Override
	public void initDefaults(JMSConfiguration config) throws IOException {
		config.putConfiguration(BROKER_URL_KEY, DEFAULT_BROKER_URL);
	}

	@Override
	public boolean override(JMSConfiguration config) throws IOException {
		return config.override(BROKER_URL_KEY, DEFAULT_BROKER_URL);
	}

}
