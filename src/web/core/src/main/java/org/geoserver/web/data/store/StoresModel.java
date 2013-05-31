/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Simple detachable model listing all the stores in a specific workspace.
 */
@SuppressWarnings("serial")
public class StoresModel extends LoadableDetachableModel<List<StoreInfo>> {

    IModel workspace;

    public StoresModel(IModel workspaceModel) {
        this.workspace = workspaceModel;
    }
    
    @Override
    protected List<StoreInfo> load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
        List stores = catalog.getStoresByWorkspace(ws, StoreInfo.class);
        Collections.sort(stores, new StoreNameComparator());
        return stores;
    }
    
    @Override
    public void detach() {
        super.detach();
        if (workspace != null) {
            workspace.detach();
        }
    }
    
    static class StoreNameComparator implements Comparator<StoreInfo> {
        public int compare(StoreInfo o1, StoreInfo o2) {
            return o1.getName().compareTo(o2.getName());
        }
        
    }
}
