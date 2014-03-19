/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.List;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;

/**
 * Configuration related
 * 
 * @author Lucas Reed, Refractions Research Inc
 */
public interface WPSInfo extends ServiceInfo {

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used during WPS
     * execute requests, when opening the connection/reading through it.
     * 
     * @return the timeout, or -1 if infinite timeout.
     */
    double getConnectionTimeout();

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite
     * timeout
     */
    void setConnectionTimeout(double timeout);

    /**
     * Returns the resource expiration timeout (in seconds). Temporary resources such as stored
     * Execute responses and output stored as reference will be deleted after this timeout
     * 
     * @return
     */
    int getResourceExpirationTimeout();

    /**
     * Sets the resource expiration timeout.
     * 
     * @param resourceExpirationTimeout
     */
    void setResourceExpirationTimeout(int resourceExpirationTimeout);

    /**
     * Returns the maximum number of processes that can run in synchronous mode in parallel.
     * Defaults to the number of available CPU cores
     * 
     * @return
     */
    public int getMaxSynchronousProcesses();

    /**
     * Sets the maximum number of processes that can run in synchronous mode in parallel.
     * 
     * @return
     */
    public void setMaxSynchronousProcesses(int maxSynchronousProcesses);

    /**
     * Returns the maximum number of processes that can run in asynchronous mode in parallel.
     * Defaults to the number of available CPU cores
     * 
     * @return
     */
    public int getMaxAsynchronousProcesses();

    /**
     * Sets the maximum number of processes that can run in asynchronous mode in parallel.
     * 
     * @param maxAsynchronousProcesses
     */
    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses);
    
    /**
     * Retrieves the process groups configurations
     * 
     * @return
     */
    public List<ProcessGroupInfo> getProcessGroups();

}
