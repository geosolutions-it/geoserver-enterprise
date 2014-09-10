/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;

/**
 * Generic interface for a process status storage. Used by ClusterProcessManager to persist process status on a shared storage used by all cluster
 * instances.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public interface ProcessStorage {

    /**
     * Removes a process from the storage. The last status is returned.
     * 
     * 
     * @param executionId process id
     * @return the execution status
     */
    public boolean remove(ProcessDescriptor process);

    /**
     * Gets the status of all executing processes on all the instances of the cluster.
     * 
     * @return the all
     */
    public Collection<ProcessDescriptor> getAll(List<ProcessState> status, String clusterID,
            Date finishedDateTimeLimit);

    public void update(ProcessDescriptor process);

    // /**
    // * Retrieves the output of a process, with the given max timeout.
    // *
    // *
    // * @return the output
    // */
    // public Map<String, Object> getOutput( String executionId, Boolean silently);
    //
    // /**
    // * Puts the output of a process on the storage.
    // *
    // *
    // * @param executionId the execution id
    // * @param status the status
    // */
    // public void putOutput( String executionId, ExecutionStatus status, Boolean silently);
    //
    // /**
    // * Puts the output error of a process on the storage.
    // *
    // *
    // * @param executionId the execution id
    // * @param e the e
    // */
    // public void putOutput( String executionId, Exception e, Boolean silently);

    /**
     * Submit.
     * 
     * 
     * @param executionId the execution id
     * @param processName the process name
     * @param background the background
     */
    public void create(ProcessDescriptor process);

    public ProcessDescriptor findByExecutionId(String executionId, Boolean silently);

    /**
     * Store result.
     * 
     * 
     * @param executionId the execution id
     * @param value the value
     */
    public void storeResult(ProcessDescriptor process, Object result);

    // /**
    // * The Class ExecutionStatusEx.
    // */
    // public static class ExecutionStatusEx extends ExecutionStatus {
    //
    // /** The result. */
    // private String result;
    //
    // public ExecutionStatusEx(ProcessDescriptor process){
    // super(
    // new NameImpl(process.getNameSpace(), process.getName()),
    // process.getExecutionId(),
    // process.getPhase(),
    // process.getProgress()
    // );
    // this.result=process.getResult();
    // }
    //
    // /**
    // * Instantiates a new execution status ex.
    // *
    // * @param status the status
    // */
    // public ExecutionStatusEx(ExecutionStatus status) {
    // super(status.getProcessName(), status.getExecutionId(), status.getPhase(), status
    // .getProgress());
    // }
    //
    // /**
    // * Instantiates a new execution status ex.
    // *
    // * @param status the status
    // * @param result the result
    // */
    // public ExecutionStatusEx(ExecutionStatus status, String result) {
    // this(status);
    // this.result = result;
    // }
    //
    // /**
    // * Sets the result.
    // *
    // * @param result the new result
    // */
    // public void setResult(String result) {
    // this.result = result;
    // }
    //
    // /**
    // * Gets the result.
    // *
    // * @return the result
    // */
    // public String getResult() {
    // return result;
    // }
    //
    // }
}
