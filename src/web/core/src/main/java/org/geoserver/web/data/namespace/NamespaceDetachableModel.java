/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.namespace;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Detachable model for a specific namespace 
 */
@SuppressWarnings("serial")
public class NamespaceDetachableModel extends LoadableDetachableModel {

    String id;
    
    public NamespaceDetachableModel( NamespaceInfo ns ) {
        this.id = ns.getId();
    }
    
    @Override
    protected Object load() {
        return GeoServerApplication.get().getCatalog().getNamespace( id );
    }

}
