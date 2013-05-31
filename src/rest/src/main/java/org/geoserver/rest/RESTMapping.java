/* Copyright (c) 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Map;

/**
 * This is the extension point for REST modules to register themselves with
 * Geoserver.  The mapping should have path specifications compatible with 
 * the REST Router class for keys, and Restlets for values.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class RESTMapping {
    private Map myRoutes;

    public void setRoutes(Map m){
        // TODO: Check this and throw an error for bad data
        myRoutes = m;
    }

    public Map getRoutes(){
        return myRoutes;
    }
}
