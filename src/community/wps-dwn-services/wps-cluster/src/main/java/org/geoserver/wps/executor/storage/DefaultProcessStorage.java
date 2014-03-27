/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.dao.ProcessDescriptorDAO;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.geotools.process.ProcessException;

import com.googlecode.genericdao.search.Search;

/**
 * The Class DefaultProcessStorage.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class DefaultProcessStorage implements ProcessStorage, ExtensionPriority {

    // /** The marshaller. */
    // private XStream marshaller = new XStream();

    /** The process descriptor dao. */
    private ProcessDescriptorDAO processDescriptorDAO;

    /**
     * Instantiates a new default process storage.
     * 
     * @param processDescriptorDAO the process descriptor dao
     */
    public DefaultProcessStorage(ProcessDescriptorDAO processDescriptorDAO) {
        this.processDescriptorDAO = processDescriptorDAO;
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.geoserver.wps.executor.ProcessStorage#putStatus(java.lang.String, java.lang.String, org.geoserver.wps.executor.ExecutionStatus)
    // */
    // /**
    // * Put status.
    // *
    // *
    // * @param executionId the execution id
    // * @param status the status
    // */
    // @Override
    // public void putStatus(String executionId, ExecutionStatus status,
    // Boolean silently) {
    // ProcessDescriptor process= getProcess( executionId, silently);
    // if(process!=null){
    // ExecutionStatus newStatus = new ExecutionStatus(status.getProcessName(), executionId,
    // status.getPhase(), status.getProgress());
    //
    // process.setPhase(status.getPhase());
    // process.setProgress(status.getProgress());
    // // process.setStatus(marshaller.toXML(newStatus));
    // process.setLastUpdateTime(new Date());
    // processDescriptorDAO.merge(process);
    // }
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.geoserver.wps.executor.ProcessStorage#getStatus(java.lang.String)
     */
    /**
     * Gets the status.
     * 
     * @param executionId the execution id
     * @return the status
     */
    @Override
    public ProcessDescriptor findByExecutionId(String executionId, Boolean silently) {
        return getProcess(executionId, silently);
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.geoserver.wps.executor.ProcessStorage#removeStatus(java.lang.String)
    // */
    // /**
    // * Removes the status.
    // *
    // *
    // * @param executionId the execution id
    // * @return the execution status
    // */
    // @Override
    // public ExecutionStatus removeProcess(String executionId, Boolean silently) {
    // ProcessDescriptor process = getProcess( executionId, true);
    // if(process==null){
    // return null;
    // }
    // ExecutionStatus status = (ExecutionStatus) marshaller.fromXML(process.getStatus());
    // if (processDescriptorDAO.remove(process)) {
    // return status;
    // }
    // return null;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.geoserver.wps.executor.ProcessStorage#getAll()
     */
    /**
     * Gets the all.
     * 
     * @return the all
     */
    @Override
    public Collection<ProcessDescriptor> getAll(List<ProcessState> status, String clusterId,
            Date finishedDateTimeLimit) {
        Search search = new Search(ProcessDescriptor.class);
        search = search.addFilterEqual("clusterId", clusterId);
        search = search.addFilterIn("phase", status);
        if (finishedDateTimeLimit != null) {
            search = search.addFilterLessOrEqual("finishTime", finishedDateTimeLimit);
        }
        return processDescriptorDAO.search(search);

    }

    /**
     * @param clusterId
     * @param executionId
     * @param silently
     * @return
     * @throws ProcessException
     */
    private ProcessDescriptor getProcess(String executionId, Boolean silently)
            throws ProcessException {
        Search search = new Search(ProcessDescriptor.class);
        search.addFilterEqual("executionId", executionId);
        search.setMaxResults(1);
        List<ProcessDescriptor> processes = processDescriptorDAO.search(search);

        if (processes == null || processes.isEmpty()) {
            if (!silently) {
                throw new ProcessException("Could not retrieve the progress of process ["
                        + executionId + "]");
            }
            return null;
        }

        ProcessDescriptor process = processes.get(0);// processDescriptorDAO.find(processes.get(0).getId());
        return process;
    }

    //
    // /*
    // * (non-Javadoc)
    // *
    // * @see org.geoserver.wps.executor.ProcessStorage#getOutput(java.lang.String, long)
    // */
    // /**
    // * Gets the output.
    // *
    // *
    // * @param executionId the execution id
    // * @param timeout the timeout
    // * @return the output
    // */
    // @Override
    // public Map<String, Object> getOutput(String executionId,Boolean silently) {
    // ProcessDescriptor process = getProcess( executionId, silently);
    // ExecutionStatus status = (ExecutionStatus) marshaller.fromXML(process.getStatus());
    // try {
    // return status.getOutput(0);
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    //
    // }

    // /**
    // * Put output.
    // *
    // *
    // * @param executionId the execution id
    // * @param status the status
    // */
    // @Override
    // public void putOutput(String executionId, ExecutionStatus status,
    // Boolean silently) {
    // ProcessDescriptor process = getProcess( executionId, silently);
    // ExecutionStatus newStatus = new ExecutionStatus(status.getProcessName(), executionId,status.getPhase(), status.getProgress());
    // process.setPhase(status.getPhase());
    // process.setProgress(status.getProgress());
    // // process.setStatus(marshaller.toXML(newStatus));
    // process.setLastUpdateTime(new Date());
    // processDescriptorDAO.merge(process);
    //
    // }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.geoserver.wps.executor.ProcessStorage#putOutput(java.lang.String, java.lang.Exception)
    // */
    // /**
    // * Put output.
    // *
    // *
    // * @param executionId the execution id
    // * @param e the e
    // */
    // @Override
    // public void putOutput(String executionId, Exception e, Boolean silently) {
    // ProcessDescriptor process = getProcess( executionId, silently);
    // Writer out = new StringWriter();
    // PrintWriter pw = new PrintWriter(out);
    // e.printStackTrace(pw);
    // process.setStatus(pw.toString());
    // process.setLastUpdateTime(new Date());
    // processDescriptorDAO.merge(process);
    //
    // }

    /**
     * Gets the priority.
     * 
     * @return the priority
     */
    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    //
    // /**
    // * Submit.
    // *
    // *
    // * @param executionId the execution id
    // * @param processName the process name
    // * @param inputs the inputs
    // * @param background the background
    // */
    // @Override
    // public ProcessDescriptor createOrFindProcess(String clusterId,String executionId, Name processName, boolean background,String email) {
    //
    // // look for an existing process (should not happen!)
    // ProcessDescriptor process=getProcess( executionId, true);
    // if (process==null) {
    // // create
    // ExecutionStatus status = new ExecutionStatus(processName, executionId, ProcessState.QUEUED, 0);
    // process = new ProcessDescriptor();
    // if(clusterId!=null&&clusterId.length()>0){
    // process.setClusterId(clusterId);
    // }
    // process.setExecutionId(executionId);
    // // process.setStatus(marshaller.toXML(status));
    // process.setProgress(0.0f);
    // process.setPhase(ProcessState.QUEUED);
    // process.setStartTime(new Date());
    // if(email!=null){
    // process.setEmail(email);
    // }
    // processDescriptorDAO.persist(process);
    // }
    // return process;
    // }

    /**
     * Store result.
     * 
     * 
     * @param executionId the execution id
     * @param result the result
     */
    @Override
    public void storeResult(ProcessDescriptor process, Object result) {

        process.setFinishTime(new Date());
        process.setLastUpdateTime(new Date());
        process.setProgress(100.0f);
        if (result instanceof File) {
            final File outputFile = (File) result;
            process.setResult(outputFile.getAbsolutePath());

        } else {
            process.setResult(result != null ? result.toString() : "");
        }

        processDescriptorDAO.merge(process);
    }

    @Override
    public void update(ProcessDescriptor process) {
        process.setLastUpdateTime(new Date());
        processDescriptorDAO.merge(process);
    }

    @Override
    public boolean remove(ProcessDescriptor process) {
        return processDescriptorDAO.remove(process);
    }

    @Override
    public void create(ProcessDescriptor process) {
        // create
        process.setStartTime(new Date());
        processDescriptorDAO.persist(process);
    }

}
