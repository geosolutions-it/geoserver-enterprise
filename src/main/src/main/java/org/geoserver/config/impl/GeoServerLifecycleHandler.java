/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.config.GeoServer;

/**
 * An extension point for classes that need to participate in the {@link GeoServer} lifecycle.
 * Any subsystem holding configuration and data/styling caches is supposed to participate in
 * this lifecycle 
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public interface GeoServerLifecycleHandler {

    /**
     * Called as part of {@link GeoServer#reset()} execution
     */
    void onReset();
    
    /**
     * Called as part of {@link GeoServer#dispose()}
     */
    void onDispose();


    /**
     * Called as part of {@link GeoServer#reload()}
     */
    void onReload();

    
    
}
