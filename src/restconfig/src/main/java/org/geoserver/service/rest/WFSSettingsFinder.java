/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.service.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.AbstractGeoServerFinder;
import org.geoserver.wfs.WFSInfo;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class WFSSettingsFinder extends AbstractGeoServerFinder {

    protected WFSSettingsFinder(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new WFSSettingsResource(getContext(), request, response, WFSInfo.class, geoServer);
    }
}
