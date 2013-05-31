/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;

/**
 * Dispatcher callback that sets and clears the {@link LocalWorkspace} and {@link LocalLayer}
 * thread locals.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LocalWorkspaceCallback implements DispatcherCallback, ExtensionPriority {

    GeoServer gs;
    Catalog catalog;
    
    public LocalWorkspaceCallback(GeoServer gs) {
        this.gs = gs;
        catalog = gs.getCatalog();
    }
    
    public Request init(Request request) {
        WorkspaceInfo ws = null;
        if (request.context != null) {
            String first = request.context;
            String last = null;
            
            int slash = first.indexOf('/');
            if (slash > -1) {
                last = first.substring(slash+1);
                first = first.substring(0, slash);
            }
            
            //check if the context matches a workspace
            ws = catalog.getWorkspaceByName(first);
            if (ws != null) {
                LocalWorkspace.set(ws);
                
                //set the local layer if it exists
                if (last != null) {
                    //hack up a qualified name
                    NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                    if (ns != null) {
                        LayerInfo l = catalog.getLayerByName(new NameImpl(ns.getURI(), last));
                        if (l != null) {
                            LocalLayer.set(l);
                        }
                        else {
                            //TODO: perhaps throw an exception?
                        }
                    }
                    
                }
            }
            else {
                //if no workspace context specified and server configuration not allowing global
                // services throw an error
                if (!gs.getGlobal().isGlobalServices()) {
                    throw new ServiceException("No such workspace '" + request.context + "'" );  
                }
            }
        }
        else if (!gs.getGlobal().isGlobalServices()) {
            throw new ServiceException("No workspace specified");
        }
        
        return request;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        return null;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        return null;
    }

    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        return null;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return null;
    }
    
    public void finished(Request request) {
        LocalWorkspace.remove();
        LocalLayer.remove();
    }

    public int getPriority() {
        return HIGHEST;
    }


}
