/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogException;


/**
 * Listener for catalog events.
 * <p>
 * <h4>Exceptions</h4>
 * In general the catalog protects itself against misbehaved listeners that throw 
 * exceptions. However sometimes it is the case in which a listener must report an error
 * via exception. For such purposes {@link CatalogException} should be thrown. 
 * </p>
 * @author   Justin Deoliveira, The Open Planning Project
 *
 */
public interface CatalogListener {

    /**
     * Handles the event of an addition to the catalog.
     */
    void handleAddEvent(CatalogAddEvent event) throws CatalogException;

    /**
     * Handles the event of a removal from the catalog.
     * 
     */
    void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException;

    /**
     * Handles the event of a modification to an object in the catalog.
     */
    void handleModifyEvent(CatalogModifyEvent event) throws CatalogException;
    
    /**
     * Handles the event of a post modification to an object in the catalog.
     */
    void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException;
    
    /**
     * A callback notifying when GeoServer configuration has been reloaded.
     * <p>
     * This method will be removed in recent version as the idea of a "reload" will not
     * exist.
     * </p>
     * @deprecated.
     */
    void reloaded();
	
}
