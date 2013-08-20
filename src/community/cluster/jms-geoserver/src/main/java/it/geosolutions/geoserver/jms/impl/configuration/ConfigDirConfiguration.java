/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Configuration class used to override the default config dir
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
// @DependsOn("JMSReadOnlyGeoServerLoader")
final public class ConfigDirConfiguration implements JMSConfigurationExt {

    @Autowired
    GeoServerResourceLoader loader;

    public ConfigDirConfiguration() throws IllegalArgumentException, IOException {
        // override default storage dir

        // final File root = GeoServerExtensions
        // .bean(GeoServerDataDirectory.class).findDataRoot();
        // if (root != null) {
    }

    @PostConstruct
    private void init() {
        JMSConfiguration.setConfigPathDir(loader.getBaseDirectory());
        // }
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        // do nothing
    }

    @Override
    public boolean checkForOverride(JMSConfiguration config) throws IOException {
        // do nothing
        return false;
    }

}
