/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Detachable model for a specific workspace. Implements IModel directly because
 * we want it to be writable as well. 
 * TODO: go back using LoadatableDetachableModel once we upgrade to Wicket 1.4, 
 * see http://issues.apache.org/jira/browse/WICKET-27 and http://issues.apache.org/jira/browse/WICKET-2364 
 */
@SuppressWarnings("serial")
public class WorkspaceDetachableModel implements IModel {
    transient WorkspaceInfo workspace;
    String id;
    
    public WorkspaceDetachableModel( WorkspaceInfo workspace ) {
        setObject(workspace);
    }
    
    public Object getObject() {
        if(workspace == null) {
            workspace = id != null 
                ? GeoServerApplication.get().getCatalog().getWorkspace( id ) : null;
        }
        return workspace;
    }

    public void setObject(Object object) {
        this.workspace = (WorkspaceInfo) object;
        this.id = workspace != null ? workspace.getId() : null;
    }

    public void detach() {
        this.workspace = null;
    }

}
