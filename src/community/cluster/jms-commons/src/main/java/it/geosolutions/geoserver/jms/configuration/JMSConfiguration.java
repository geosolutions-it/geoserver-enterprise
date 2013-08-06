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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.WebUtils;

/**
 * 
 * Abstract class to store and load configuration from global var or temp webapp
 * directory
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
// @org.springframework.context.annotation.Configuration("JMSConfiguration")
// @org.springframework.context.annotation.Configuration("JMSConfiguration")
final public class JMSConfiguration {
	protected static final Logger LOGGER = LoggerFactory
			.getLogger(JMSConfiguration.class);

	@Autowired
	public List<JMSConfigurationExt> exts;

	public static final String INSTANCE_NAME_KEY = "instanceName";

	// This file will be stored into the webapp temp dir to save the
	// INSTANCE_NAME and the TOPIC_NAME
	public static final String CONFIG_FILE_NAME = "cluster.properties";

	// the configuration
	protected Properties configuration = new Properties();

	public Properties getConfigurations() {
		return configuration;
	}

	public <T> T getConfiguration(String key) {
		return (T) configuration.get(key);
	}

	public void putConfiguration(String key, Object o) {
		configuration.put(key, o);
	}

	 public final String getInstanceName() {
	 return configuration.getProperty(INSTANCE_NAME_KEY);
	 }

	/**
	 * Initialize configuration
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	private void init() throws IOException {
		try {
			loadTempConfig();

			// if configuration is changed (since last boot) store changes
			// on disk
			if (checkForOverride(this)) {
				storeTempConfig();
			}

		} catch (FileNotFoundException e) {

			initDefaults(this);
			storeTempConfig();
		}
	}

	/**
	 * Initialize configuration with default parameters
	 * 
	 * @throws IOException
	 */
//	@Override
	public void initDefaults(JMSConfiguration config) throws IOException {
		// set the name
		configuration.put(
				INSTANCE_NAME_KEY,UUID.randomUUID()
						.toString());
		if (exts != null) {
			for (JMSConfigurationExt ext : exts) {
				ext.initDefaults(this);
			}
		}
	}

	/**
	 * check if instance name is changed since last application boot
	 * 
	 * @return true if some parameter is overridden by an Extension property
	 */
//	@Override
	public boolean checkForOverride(JMSConfiguration config) {
		return checkForOverride(INSTANCE_NAME_KEY, UUID.randomUUID().toString());
	}

	public final boolean checkForOverride(String nameKey, String defaultVal) {
		boolean override = false;
		final String ovrName = getExtensionsProp(nameKey);
		if (ovrName != null) {
			final String name = configuration.getProperty(nameKey);
			if (name != null && !name.equals(ovrName)) {
				override = true;
			}
			configuration.put(nameKey, ovrName);
		} else {
			final String name = configuration.getProperty(nameKey);
			if (name != null) {
				configuration.put(nameKey, name);
			} else {
				override = true;
				configuration.put(nameKey, defaultVal);
			}
		}
		
		return override;
	}

	public <T> T getExtensionsProp(final String theName) {
		return (T) ApplicationProperties.getProperty(theName);
	}

	public void loadTempConfig() throws FileNotFoundException, IOException {
		final File config = new File(getTempDir(), CONFIG_FILE_NAME);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(config);
			this.configuration.load(fis);
		} finally {
			if (fis != null)
				fis.close();
		}
	}

	public void storeTempConfig() throws IOException {
		final File config = new File(getTempDir(), CONFIG_FILE_NAME);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(config);
			this.configuration.store(fos, "");
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public File getTempDir() {
		String tempPath = ApplicationProperties
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
