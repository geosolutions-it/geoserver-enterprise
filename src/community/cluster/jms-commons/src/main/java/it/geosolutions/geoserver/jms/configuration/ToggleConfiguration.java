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

	public static final String TOGGLE_PRODUCER_KEY = "toggleProducer";
	public static final String DEFAULT_PRODUCER_STATUS = "true";

	public static final String TOGGLE_CONSUMER_KEY = "toggleConsumer";
	public static final String DEFAULT_CONSUMER_STATUS = "true";

	@Override
	public void initDefaults(JMSConfiguration config) {
		config.putConfiguration(TOGGLE_PRODUCER_KEY, DEFAULT_PRODUCER_STATUS);
		config.putConfiguration(TOGGLE_CONSUMER_KEY, DEFAULT_CONSUMER_STATUS);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) {
		return config.checkForOverride(TOGGLE_PRODUCER_KEY,
				DEFAULT_PRODUCER_STATUS)
				&& config.checkForOverride(TOGGLE_CONSUMER_KEY,
						DEFAULT_CONSUMER_STATUS);
	}

}
