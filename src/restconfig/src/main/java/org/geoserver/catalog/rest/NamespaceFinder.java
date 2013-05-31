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

public class NamespaceFinder extends AbstractCatalogFinder {

    public NamespaceFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String namespace = getAttribute(request, "namespace");
        
        if ( namespace == null && request.getMethod() == Method.GET ) {
            return new NamespaceListResource( getContext(), request, response, catalog );
        }
        
        if ( namespace != null ) {
            //ensure it exists
            if ( catalog.getNamespaceByPrefix( namespace ) == null ) {
                throw new RestletException( "No such namespace: " + namespace, Status.CLIENT_ERROR_NOT_FOUND );
            }
        }
        
        return new NamespaceResource( null, request, response, catalog );
    }

}
