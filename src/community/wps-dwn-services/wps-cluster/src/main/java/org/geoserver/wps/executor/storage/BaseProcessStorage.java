/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;

/**
 * Basic Implementation for a {@link ProcessStorage} that actually does nothing
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class BaseProcessStorage implements ProcessStorage {

    @Override
    public Collection<ProcessDescriptor> getAll(List<ProcessState> status, String clusterID,
            Date finishedDateTimeLimit) {
        return Collections.emptyList();
    }

    @Override
    public void update(ProcessDescriptor process) {
    }

    @Override
    public boolean remove(ProcessDescriptor process) {
        return false;
    }

    @Override
    public void create(ProcessDescriptor process) {
    }

    @Override
    public ProcessDescriptor findByExecutionId(String executionId, Boolean silently) {
        return null;
    }

    @Override
    public void storeResult(ProcessDescriptor process, Object result) {
        // TODO Auto-generated method stub

    }
}
