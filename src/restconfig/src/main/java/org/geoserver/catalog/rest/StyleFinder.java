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

public class StyleFinder extends AbstractCatalogFinder {

    public StyleFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String workspace = getAttribute(request, "workspace");
        String style = getAttribute(request, "style");
        String layer = getAttribute(request, "layer");

        //check if workspace exists
        if (workspace != null && catalog.getWorkspaceByName(workspace) == null) {
            throw new RestletException( "No such workspace: " + workspace, Status.CLIENT_ERROR_NOT_FOUND );
        }
        //check style exists if specified
        if ( style != null) {
            if (workspace != null && catalog.getStyleByName( workspace, style ) == null) {
                throw new RestletException(String.format("No such style %s in workspace %s", 
                    style, workspace), Status.CLIENT_ERROR_NOT_FOUND );
            }
            if (workspace == null && catalog.getStyleByName( style ) == null) {
                throw new RestletException( "No such style: " + style, Status.CLIENT_ERROR_NOT_FOUND );
            }
        }

        //check layer exists if specified
        if ( layer != null && catalog.getLayerByName( layer ) == null ) {
            throw new RestletException( "No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND);
            /*
            String ns = null;
            String resource = null;
            
            if ( layer.contains( ":" ) ) {
                String[] split = layer.split(":");
                ns = split[0];
                resource = split[1];
            }
            else {
                ns = catalog.getDefaultNamespace().getPrefix();
                resource = layer;
            }
            
            if ( catalog.getResourceByName( ns, resource, ResourceInfo.class) == null ) {
                throw new RestletException( "No such layer: " + ns + "," + resource, Status.CLIENT_ERROR_NOT_FOUND);
            }
            
            //set the parsed result as request attributes
            request.getAttributes().put( "namespace", ns );
            request.getAttributes().put( "resource", resource );
            */
        }
        
        if ( style == null && request.getMethod() == Method.GET ) {
            return new StyleListResource(getContext(),request,response,catalog);
        }
        
        return new StyleResource(getContext(),request,response,catalog);
    }

}
