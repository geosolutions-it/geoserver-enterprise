/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ConfigurationException;

import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.ProcessStorage;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;

/**
 * Cleans up the temporary storage directory for WPS. Extends the wps core WPSStorageCleaner looking for old storage records. Moreover if a file has
 * been associated to an event, it will be removed on file expiration.
 * 
 * Expiration time taken from WPS Service.
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Alessio Fabiani - GeoSolutions
 */
public class WPSClusterStorageCleaner extends WPSStorageCleaner {

    private final static Logger LOGGER = Logging.getLogger(WPSClusterStorageCleaner.class);

    /** The available storages. */
    private ProcessStorage processStorage;

    /** Is Enabled or not. */
    private boolean enabled;

    private String clusterId;

    public WPSClusterStorageCleaner(WPSResourceManager resourceManager,
            ProcessStorage processStorage, String clusterid) throws IOException,
            ConfigurationException {
        super(resourceManager.getWpsOutputStorage());
        this.processStorage = processStorage;
        this.clusterId = clusterid;

        // if no storage is available just initialize with the stub one
        if (processStorage == null) {
            throw new IllegalArgumentException("Null ProcessStorage provided.");
        }
    }

    @Override
    public void run() {
        try {
            if (!getStorage().exists())
                return;

            // ok, now scan for existing files there and clean up those
            // that are too old
            long now = System.currentTimeMillis();
            cleanupDirectory(getStorage(), now); // call parent method
            cleanupStorage(now);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to clean up "
                    + "old files from temp storage", e);
        }
    }

    /**
     * Recursively cleans up files that are too old
     * 
     * @param directory
     * @param now
     * @throws IOException
     */
    private void cleanupStorage(long now) throws IOException {
        if (enabled) {
            final Collection<ProcessDescriptor> processes = processStorage.getAll(Arrays.asList(
                    ProcessState.COMPLETED, ProcessState.CANCELLED, ProcessState.FAILED),
                    clusterId, new Date(now - expirationDelay));
            for (ProcessDescriptor process : processes) {
                // get process
                processStorage.remove(process);
            }
        }
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
