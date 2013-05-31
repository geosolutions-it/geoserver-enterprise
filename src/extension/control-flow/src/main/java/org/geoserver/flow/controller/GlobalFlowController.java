/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/**
 * A flow controller matching all requests, can be used for globally controlling the number of
 * incoming requests
 * 
 * @author Andrea Aime - OpenGeo
 */
public class GlobalFlowController extends SingleQueueFlowController {

    public GlobalFlowController(int queueSize) {
        super(queueSize);
    }

    @Override
    protected boolean matchesRequest(Request request) {
        return true;
    }
    
    @Override
    public String toString() {
        return "GlobalFlowController(" + queueSize + ")";
    }

}
