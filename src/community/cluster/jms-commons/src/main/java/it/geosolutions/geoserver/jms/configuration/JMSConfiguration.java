/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.WebUtils;

/**
 * 
 * Abstract class to store and load configuration from global var or temp webapp directory
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class JMSConfiguration {
    public static final String DEFAULT_GROUP = "geoserver-cluster";

	protected static final java.util.logging.Logger LOGGER = Logging.getLogger(JMSConfiguration.class);

    @Autowired
    public List<JMSConfigurationExt> exts;

    public static final String INSTANCE_NAME_KEY = "instanceName";
    
    public static final String GROUP_KEY = "group";

    /**
     * This file contains the configuration
     */
    public static final String CONFIG_FILE_NAME = "cluster.properties";

    /**
     * This variable stores the configuration path dir. the default initialization will set this to the webapp temp dir. If you need to store it to a
     * new path use the setter to change it.
     */
    private static File configPathDir = getTempDir();

    public static void setConfigPathDir(File dir) {
        configPathDir = dir;
    }

    public static final File getConfigPathDir() {
        return configPathDir;
    }

    // the configuration
    protected Properties configuration = new Properties();

    public Properties getConfigurations() {
        return configuration;
    }

    public <T> T getConfiguration(String key) {
        return (T) configuration.get(key);
    }

    public void putConfiguration(String key, String o) {
        configuration.put(key, o);
    }

    /**
     * Initialize configuration
     * 
     * @throws IOException
     */
    @PostConstruct
    private void init() throws IOException {
        try {
            loadConfig();
            if (configuration.isEmpty()) {
                initDefaults();
            }
            // if configuration is changed (since last boot) store changes
            // on disk
            boolean override = override();
            if (exts != null) {
                for (JMSConfigurationExt ext : exts) {
                    override |= ext.override(this);
                }
            }
            if (override) {
                storeConfig();
            }

        } catch (IOException e) {
            LOGGER.severe("Unable to load properties: using defaults");
            initDefaults();
        }

        try {
            storeConfig();
        } catch (IOException e) {
            LOGGER.severe("Unable to store properties");
        }

    }

    /**
     * Initialize configuration with default parameters
     * 
     * @throws IOException
     */
    public void initDefaults() throws IOException {
    	// set the group
    	configuration.put(GROUP_KEY, DEFAULT_GROUP);
    	
        // set the name
        configuration.put(INSTANCE_NAME_KEY, UUID.randomUUID().toString());
        if (exts != null) {
            for (JMSConfigurationExt ext : exts) {
                ext.initDefaults(this);
            }
        }
    }

    /**
     * check if instance name is changed since last application boot, if so set the overridden value into configuration and returns true
     * 
     * @return true if some parameter is overridden by an Extension property
     */
    public boolean override() {
        return override(INSTANCE_NAME_KEY, UUID.randomUUID().toString());
    }

    /**
     * check if instance name is changed since last application boot, if so set the overridden value into configuration and returns true
     */
    public final boolean override(String nameKey, Object defaultVal) {
        boolean override = false;
        final String ovrName = getOverride(nameKey);
        if (ovrName != null) {
            final String name = configuration.getProperty(nameKey);
            if (name != null && !name.equals(ovrName)) {
                override = true;
            }
            configuration.put(nameKey, ovrName);
        } else {
            final String name = configuration.getProperty(nameKey);
            // no configuration found setup defaults and return override==true
            if (name == null) {
                override = true;
                configuration.put(nameKey, defaultVal);
            }
        }
        return override;
    }

    /**
     * @param theName the key name of the configuration parameter
     * @return the overridden value if override is set, null otherwise
     */
    public static <T> T getOverride(final String theName) {
        return (T) ApplicationProperties.getProperty(theName);
    }

    public void loadConfig() throws IOException {
        final File config = new File(configPathDir, CONFIG_FILE_NAME);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(config);
            this.configuration.load(fis);
        } finally {
            if (fis != null)
                fis.close();
        }
    }

    public void storeConfig() throws IOException {
        final File config = new File(configPathDir, CONFIG_FILE_NAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(config);
            this.configuration.store(fos, "");
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    public final static File getTempDir() {
        String tempPath = ApplicationProperties.getProperty(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE);
        if (tempPath == null) {
            return null;
        }
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
