/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.Configuration;
import it.geosolutions.geoserver.jms.configuration.ConfigurationExt;

/**
 * 
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ServerConfiguration implements ConfigurationExt {

	public static final String TOGGLE_PRODUCER_KEY = "toggleProducer";
	public static final String DEFAULT_PRODUCER_STATUS = "true";

	@Override
	public void initDefaults(Configuration config) {
		config.putConfiguration(TOGGLE_PRODUCER_KEY, DEFAULT_PRODUCER_STATUS);
	}

	@Override
	public boolean checkForOverride(Configuration config) {
		return config.checkForOverride(TOGGLE_PRODUCER_KEY,
					DEFAULT_PRODUCER_STATUS);
	}

}
