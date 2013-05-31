/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class CoverageStoreFinder extends AbstractCatalogFinder {

    public CoverageStoreFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String ws = getAttribute(request, "workspace");
        String cs = getAttribute(request, "coveragestore");
        
        //ensure referenced resources exist
        if ( ws != null && catalog.getWorkspaceByName( ws ) == null ) {
            throw new RestletException( "No such workspace: " + ws, Status.CLIENT_ERROR_NOT_FOUND );
        }
        if ( cs != null && catalog.getCoverageStoreByName(ws, cs) == null ) {
            throw new RestletException( "No such coverage store: " + ws + "," + cs, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        if ( cs == null && request.getMethod() == Method.GET ) {
            return new CoverageStoreListResource(getContext(),request,response,catalog);
        }
        return new CoverageStoreResource( null, request, response, catalog );
    }
}
