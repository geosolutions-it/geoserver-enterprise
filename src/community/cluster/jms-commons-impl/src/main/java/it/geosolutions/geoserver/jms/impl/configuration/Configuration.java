/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.geoserver.platform.GeoServerExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.WebUtils;

/**
 * 
 * Abstract class to store and load configuration from global var or
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public abstract class Configuration extends
		it.geosolutions.geoserver.jms.configuration.Configuration {
	protected static final Logger LOGGER = LoggerFactory
			.getLogger(Configuration.class);

	public static final String TOPIC_NAME = "TOPIC_NAME";
	public static final String DEFAULT_TOPIC_NAME = ">";

	public static String getTopicName() {
		return configuration.getProperty(TOPIC_NAME);
	}

	static {
		try {

			boolean override = false;

			// check if topic is changed since last application boot
			String topic = getExtensionsProp(TOPIC_NAME, DEFAULT_TOPIC_NAME);
			if (topic != null
					&& !configuration.getProperty(TOPIC_NAME).equals(topic)) {
				override = true;
				configuration.put(TOPIC_NAME, topic);
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
