/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import java.util.List;

/**
 * Event for the modification of an object in the catalog.
 * <p>
 * The {@link #getSource()} method returns the object unmodified. For access to the object 
 * after it has been modified, see {@link CatalogPostModifyEvent}.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface CatalogModifyEvent extends CatalogEvent {

    /**
     * The names of the properties that were modified.
     */
    List<String> getPropertyNames();

    /**
     * The old values of the properties that were modified.
     */
    List<Object> getOldValues();

    /**
     * The new values of the properties that were modified.
     */
    List<Object> getNewValues();
}
