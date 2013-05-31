/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class WMSLayerListResource extends AbstractCatalogListResource {

    public WMSLayerListResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, WMSLayerInfo.class, catalog);
    }

    @Override
    protected List handleListGet() throws Exception {
        String ws = getAttribute( "workspace" ); 
        String wms = getAttribute( "wmsstore" );
        
        if ( wms != null ) {
            WMSStoreInfo dataStore = catalog.getStoreByName( wms, WMSStoreInfo.class );
            return catalog.getResourcesByStore(dataStore, WMSLayerInfo.class);    
        }
        
        NamespaceInfo ns = catalog.getNamespaceByPrefix( ws );
        return catalog.getResourcesByNamespace( ns , WMSLayerInfo.class );
    }

}
