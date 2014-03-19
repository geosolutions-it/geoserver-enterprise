/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * The Class TestProcessStorage.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class TestProcessStorage implements ProcessStorage, ExtensionPriority,
        ApplicationListener<ApplicationEvent> {

    private boolean testMode = false;

    /**
     * On application event.
     * 
     * @param event the event
     */
    public void onApplicationEvent(ApplicationEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the priority.
     * 
     * @return the priority
     */
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    /**
     * Put status.
     * 
     * 
     * @param executionId the execution id
     * @param status the status
     */
    public void putStatus(String executionId, ExecutionStatus status, Boolean silently) {
        // TODO Auto-generated method stub

    }

    /**
     * Removes the status.
     * 
     * 
     * @param executionId the execution id
     * @return the execution status
     */
    public ExecutionStatus removeProcess(String executionId, Boolean silently) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the all.
     * 
     * @return the all
     */
    public Collection<ProcessDescriptor> getAll(List<ProcessState> status, String clusterId,
            Date finishedDateTimeLimit) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update phase.
     * 
     * 
     * @param executionId the execution id
     * @param phase the phase
     */
    public void updatePhase(String executionId, ProcessState phase, Boolean silently) {
        // TODO Auto-generated method stub

    }

    /**
     * Update progress.
     * 
     * 
     * @param executionId the execution id
     * @param progress the progress
     */
    public void updateProgress(String executionId, float progress, Boolean silently) {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the output.
     * 
     * 
     * @param executionId the execution id
     * @return the output
     */
    public Map<String, Object> getOutput(String executionId, Boolean silently) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the single instance of TestProcessStorage.
     * 
     * @param executionId the execution id
     * @return single instance of TestProcessStorage
     */
    public String getInstance(String executionId, Boolean silently) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Put output.
     * 
     * 
     * @param executionId the execution id
     * @param status the status
     */
    public void putOutput(String executionId, ExecutionStatus status, Boolean silently) {
        // TODO Auto-generated method stub

    }

    /**
     * Put output.
     * 
     * 
     * @param executionId the execution id
     * @param e the e
     */
    public void putOutput(String executionId, Exception e, Boolean silently) {
        // TODO Auto-generated method stub

    }

    /**
     * Submit.
     * 
     * 
     * @param executionId the execution id
     * @param processName the process name
     * @param inputs the inputs
     * @param background the background
     */
    public ProcessDescriptor createOrFindProcess(String clusterId, String executionId,
            Name processName, boolean background, String email) {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * Submit chained.
     * 
     * 
     * @param executionId the execution id
     * @param processName the process name
     * @param inputs the inputs
     */
    public void submitChained(String executionId, Name processName, Map<String, Object> inputs) {
        // TODO Auto-generated method stub

    }

    /**
     * Store result.
     * 
     * 
     * @param executionId the execution id
     * @param value the value
     */
    public void storeResult(String executionId, Object value, Boolean silently) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update(ProcessDescriptor process) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean remove(ProcessDescriptor process) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void create(ProcessDescriptor process) {
        // TODO Auto-generated method stub

    }

    @Override
    public ProcessDescriptor findByExecutionId(String executionId, Boolean silently) {
        
        if (isTestMode()) {
            ProcessDescriptor ps = new ProcessDescriptor();
            ps.setExecutionId(executionId);
            return ps;
        }
        return null;
    }

    @Override
    public void storeResult(ProcessDescriptor process, Object result) {
        // TODO Auto-generated method stub

    }

    /**
     * @param testMode the testMode to set
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * @return the testMode
     */
    public boolean isTestMode() {
        return testMode;
    }

}
