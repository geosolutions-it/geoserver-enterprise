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
	public static final String DEFAULT_MASTER_STATUS = "true";

	public static final String TOGGLE_SLAVE_KEY = "toggleSlave";
	public static final String DEFAULT_SLAVE_STATUS = "true";

	@Override
	public void initDefaults(JMSConfiguration config) {
		config.putConfiguration(TOGGLE_MASTER_KEY, DEFAULT_MASTER_STATUS);
		config.putConfiguration(TOGGLE_SLAVE_KEY, DEFAULT_SLAVE_STATUS);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) {
		return config.checkForOverride(TOGGLE_MASTER_KEY,
				DEFAULT_MASTER_STATUS)
				|| config.checkForOverride(TOGGLE_SLAVE_KEY,
						DEFAULT_SLAVE_STATUS);
	}

}
