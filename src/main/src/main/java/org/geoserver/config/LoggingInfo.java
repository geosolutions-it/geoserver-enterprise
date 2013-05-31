/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Info;

/**
 * Logging configuration.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface LoggingInfo extends Info {

    /**
     * The GeoServer logging level.
     * <p>
     * The name of the level is specific to GeoServer, and independent of the 
     * actual logging framework.
     * </p>
     */
    String getLevel();
    
    /**
     * Sets the logging level.
     */
    void setLevel( String loggingLevel );
    
    /**
     * The location where GeoServer logs to.
     * <p>
     * This value is intended to be used by adminstrators who require logs to be
     * written in a particular location.
     * </p>
     */
    String getLocation();
    
    /**
     * Sets the logging location.
     * 
     * @param loggingLocation A file or url to a location to log.
     */
    void setLocation( String loggingLocation );
    
    /**
     * Flag indicating if GeoServer logs to stdout.
     */
    boolean isStdOutLogging();
    
    /**
     * Sets stdout logging flag.
     */
    void setStdOutLogging( boolean supressStdOutLogging );
}
