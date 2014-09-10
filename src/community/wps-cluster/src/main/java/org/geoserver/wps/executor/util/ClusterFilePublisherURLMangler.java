/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.util;

import java.io.File;

/**
 * Generinc interface for Cluster File Publishing extensions.
 * 
 * It can be used to plug into GeoServer custom URL manglers for the publishing of processes outputs files.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public interface ClusterFilePublisherURLMangler {

    /**
     * Gets the publishing url.
     * 
     * @param file the file
     * @return the publishing url
     * @throws Exception the exception
     */
    public String getPublishingURL(File file, String baseURL) throws Exception;

}
