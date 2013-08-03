/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.Configuration;

import java.io.IOException;

/**
 * 
 * Abstract class to store and load configuration from global var or
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public abstract class ConfigurationImpl extends Configuration {

	public static final String TOPIC_NAME_KEY = "TOPIC_NAME";
	public static final String DEFAULT_TOPIC_NAME = ">";

	public static String getTopicName() {
		return configuration.getProperty(TOPIC_NAME_KEY);
	}

	static {
		try {

			boolean override = false;

			// check if topic is overridden or is changed since last application
			// boot
			final String topic = configuration.getProperty(TOPIC_NAME_KEY);
			if (topic != null) {
				// get override or default
				final String ovrTopic = getExtensionsProp(TOPIC_NAME_KEY,
						DEFAULT_TOPIC_NAME);

				// if topic is overridden or there's a default value
				if (ovrTopic != null && !ovrTopic.equals(topic)) {
					override = true;
					configuration.put(TOPIC_NAME_KEY, topic);
				} else {
					// never here
					throw new IllegalStateException("Unable to load the "
							+ TOPIC_NAME_KEY);
				}
			}

			// if configuration is changed store changes on disk
			if (override) {
				storeTempConfig(configuration);
			}

		} catch (IOException e) {
			throw new IllegalStateException(
					"Unable to load temporary configuration");
		}
	}

}
