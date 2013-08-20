/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

/**
 * 
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ToggleConfiguration implements JMSConfigurationExt {

	public static final String TOGGLE_MASTER_KEY = "toggleMaster";
	public static final String DEFAULT_PRODUCER_STATUS = "true";

	public static final String TOGGLE_SLAVE_KEY = "toggleSlave";
	public static final String DEFAULT_CONSUMER_STATUS = "true";

	@Override
	public void initDefaults(JMSConfiguration config) {
		config.putConfiguration(TOGGLE_MASTER_KEY, DEFAULT_PRODUCER_STATUS);
		config.putConfiguration(TOGGLE_SLAVE_KEY, DEFAULT_CONSUMER_STATUS);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) {
		return config.checkForOverride(TOGGLE_MASTER_KEY,
				DEFAULT_PRODUCER_STATUS)
				|| config.checkForOverride(TOGGLE_SLAVE_KEY,
						DEFAULT_CONSUMER_STATUS);
	}

}
