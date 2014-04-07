/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Configuration class used to override the default config dir (GEOSERVER_DATA_DIR/cluster/)
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class ConfigDirConfiguration implements JMSConfigurationExt {

    @Autowired
    GeoServerResourceLoader loader;

    public static final String CONFIGDIR_KEY = "CLUSTER_CONFIG_DIR";

    /**
     * Override the global config dir
     * @throws IOException
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void init() throws IOException {
        // check for override
        File baseDir=null;
        final String baseDirPath = JMSConfiguration.getOverride(CONFIGDIR_KEY);
        // if no override try to load from the GeoServer loader
        if (baseDirPath != null) {
            baseDir = new File(baseDirPath);
        } else {
            baseDir = loader.getBaseDirectory();
            if (baseDir != null) {
                baseDir = new File(baseDir, "cluster");
            }
        }
        if (baseDir != null) {
            if (!baseDir.exists() && !baseDir.mkdir()) {
                throw new IOException("Unable to create directory: " + baseDir);
            }
        }
        JMSConfiguration.setConfigPathDir(baseDir);
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(CONFIGDIR_KEY, JMSConfiguration.getConfigPathDir().toString());
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(CONFIGDIR_KEY, JMSConfiguration.getConfigPathDir().toString());
    }

}
