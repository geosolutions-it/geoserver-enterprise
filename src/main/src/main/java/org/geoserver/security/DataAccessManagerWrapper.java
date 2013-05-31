/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.springframework.security.core.Authentication;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Abstract class for wrappers around an existing data access manager.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class DataAccessManagerWrapper implements DataAccessManager {

    protected DataAccessManager delegate;
  
    public void setDelegate(DataAccessManager delegate) {
        this.delegate = delegate;
    }
    
    public boolean canAccess(Authentication user, WorkspaceInfo workspace, AccessMode mode) {
        return delegate.canAccess(user, workspace, mode);
    }

    public boolean canAccess(Authentication user, LayerInfo layer, AccessMode mode) {
        return delegate.canAccess(user, layer, mode);
    }

    public boolean canAccess(Authentication user, ResourceInfo resource, AccessMode mode) {
        return delegate.canAccess(user, resource, mode);
    }

    public CatalogMode getMode() {
        return delegate.getMode();
    }

}
