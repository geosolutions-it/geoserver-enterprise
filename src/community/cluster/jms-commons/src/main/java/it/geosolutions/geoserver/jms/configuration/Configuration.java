/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

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
 * Abstract class to store and load configuration from global var or temp webapp
 * directory
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public abstract class Configuration {
	protected static final Logger LOGGER = LoggerFactory
			.getLogger(Configuration.class);

	public static final String INSTANCE_NAME_KEY = "INSTANCE_NAME";

	public static final String getInstanceName() {
		return configuration.getProperty(INSTANCE_NAME_KEY);
	}

	// This file will be stored into the webapp temp dir to save the
	// INSTANCE_NAME and the TOPIC_NAME
	public static final String CONFIG_FILE_NAME = "cluster.properties";

	// the configuration
	protected static Properties configuration;

	public static Properties getProperties() {
		return configuration;
	}

	/**
	 * Initialize configuration
	 */
	static {
		try {
			configuration = loadTempConfig();

			if (configuration == null) {
				configuration = new Properties();

				// initDefaults();
				// set the name
				configuration.put(
						INSTANCE_NAME_KEY,
						getExtensionsProp(INSTANCE_NAME_KEY, UUID.randomUUID()
								.toString()));

			} else {
				// if configuration is changed (since last boot) store changes
				// on disk
				// if (checkForOverride()) {
				// storeTempConfig(configuration);
				// }

				final String name = getExtensionsProp(INSTANCE_NAME_KEY, UUID
						.randomUUID().toString());
				if (name != null
						&& !configuration.getProperty(INSTANCE_NAME_KEY)
								.equals(name)) {
					configuration.put(INSTANCE_NAME_KEY, name);

					// store config changes
					storeTempConfig(configuration);
				}

			}
		} catch (IOException e) {
			throw new IllegalStateException(
					"Unable to load temporary configuration");
		}
	}

	// /**
	// * Initialize configuration with default parameters
	// */
	// protected static void initDefaults() {
	// // set the name
	// configuration.put(
	// INSTANCE_NAME_KEY,
	// getExtensionsProp(INSTANCE_NAME_KEY, UUID.randomUUID()
	// .toString()));
	//
	// }
	//
	// /**
	// * check if instance name is changed since last application boot
	// * @return true if some parameter is overridden by an Extension property
	// */
	// protected static boolean checkForOverride() {
	// final String name = getExtensionsProp(INSTANCE_NAME_KEY,
	// UUID.randomUUID()
	// .toString());
	// if (name != null
	// && !configuration.getProperty(INSTANCE_NAME_KEY).equals(name)) {
	// configuration.put(INSTANCE_NAME_KEY, name);
	// return true;
	// }
	// return false;
	// }

	protected static String getExtensionsProp(final String theName,
			final String defaultVal) {
		final String name = GeoServerExtensions.getProperty(theName);
		if (name == null) {
			LOGGER.warn("No " + theName
					+ " extension property found. Using default: " + defaultVal);
			return defaultVal;
		}
		return name;
	}

	protected static Properties loadTempConfig() throws IOException {
		final File config = new File(getTempDir(), CONFIG_FILE_NAME);
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(config);
			props.load(fis);
		} catch (FileNotFoundException e) {
			return null;
		} finally {
			if (fis != null)
				fis.close();
		}
		return props;
	}

	protected static void storeTempConfig(final Properties props)
			throws IOException {
		final File config = new File(getTempDir(), CONFIG_FILE_NAME);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(config);
			props.store(fos, "");
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public static File getTempDir() {
		String tempPath = GeoServerExtensions
				.getProperty(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE);
		if (tempPath == null)
			return null;
		File tempDir = new File(tempPath);
		if (tempDir.exists() == false)
			return null;
		if (tempDir.isDirectory() == false)
			return null;
		if (tempDir.canWrite() == false)
			return null;
		return tempDir;
	}

}
