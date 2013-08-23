/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ReadOnlyConfiguration;
import it.geosolutions.geoserver.jms.configuration.ReadOnlyConfiguration.ReadOnlyConfigurationStatus;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * A Read Only persister which inhibits write operation on the disk (if enabled)<br/>
 * 
 * Note: to work this class is declared in the same package of the extending one since some member are declared as package protected.
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
public class ReadOnlyGeoServerLoader extends DefaultGeoServerLoader {

    private Boolean enabled = false;

    @Autowired
    public JMSConfiguration config;

    public ReadOnlyGeoServerLoader(final GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @PostConstruct
    private void init() {
        Object statusObj = config.getConfiguration(ReadOnlyConfiguration.READ_ONLY_KEY);
        if (statusObj == null) {
            statusObj = ReadOnlyConfiguration.DEFAULT_READ_ONLY_VALUE;
        }
        enabled = ReadOnlyConfigurationStatus.enabled.equals(statusObj.toString());
    }

    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        synchronized (enabled) {
            if (enabled) {
                catalog.setResourceLoader(resourceLoader);
                readCatalog(catalog, xp);
            } else {
                super.loadCatalog(catalog, xp);
            }
        }
    }

    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
        synchronized (enabled) {
            if (enabled) {
                readConfiguration(geoServer, xp);
            } else {
                super.loadGeoServer(geoServer, xp);
            }
        }
    }

    @Override
    protected void initializeStyles(Catalog catalog, XStreamPersister xp) throws IOException {
        super.initializeStyles(catalog, xp);
    }

    public boolean isEnabled() {
        synchronized (enabled) {
            return enabled;
        }
    }

    public void setEnabled(boolean enabled) {
        synchronized (this.enabled) {
            this.enabled = enabled;
            if (enabled) {
                // remove Default persister
                if (persister != null) {
                    geoserver.removeListener(persister);
                    persister = null;
                }
                // remove Default listener
                if (listener != null) {
                    geoserver.removeListener(listener);
                    listener = null;
                }
            } else {
                if (listener == null) {
                    // add event listener which persists changes
                    final List<XStreamServiceLoader> loaders = GeoServerExtensions
                            .extensions(XStreamServiceLoader.class);
                    listener = new ServicePersister(loaders, geoserver);
                    geoserver.addListener(listener);
                }
                if (persister == null) {
                    persister = new GeoServerPersister(resourceLoader, xpf.createXMLPersister());
                    // attach back the persister
                    geoserver.addListener(persister);
                }
            }
        }
    }
}
