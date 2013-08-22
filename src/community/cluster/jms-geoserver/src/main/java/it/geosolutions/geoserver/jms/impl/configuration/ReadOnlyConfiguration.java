/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import java.io.IOException;

import org.geoserver.config.ReadOnlyGeoServerLoader;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;

/**
 * 
 * class to store and load configuration for {@link ReadOnlyGeoServerLoader}
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ReadOnlyConfiguration implements JMSConfigurationExt {

	public static final String READ_ONLY_KEY = "readOnly";
	public static final String DEFAULT_READ_ONLY_VALUE = ReadOnlyConfigurationStatus.disabled.toString();
	
	public static enum ReadOnlyConfigurationStatus {
	    enabled,
	    disabled;
	}

	@Override
	public void initDefaults(JMSConfiguration config) throws IOException {
		config.putConfiguration(READ_ONLY_KEY, DEFAULT_READ_ONLY_VALUE);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) throws IOException {
		return config.checkForOverride(READ_ONLY_KEY, DEFAULT_READ_ONLY_VALUE);
	}

}
