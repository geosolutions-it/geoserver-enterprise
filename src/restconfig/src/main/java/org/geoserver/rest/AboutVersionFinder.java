/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest;

import org.geoserver.ManifestLoader.AboutModel.AboutModelType;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
public class AboutVersionFinder extends Finder {

    protected AboutVersionFinder() {
        super();
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new AboutManifest(getContext(), request, response, AboutModelType.VERSIONS);
    }
}
