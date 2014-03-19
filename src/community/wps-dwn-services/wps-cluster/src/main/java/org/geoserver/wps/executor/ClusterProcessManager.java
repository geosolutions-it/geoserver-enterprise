/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.ProcessStorage;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.geoserver.wps.executor.util.ClusterFilePublisherURLMangler;
import org.geoserver.wps.mail.SendMail;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/**
 * Alternative implementation of ProcessManager, using a storage (ProcessStorage) to share process status between the instances of a cluster.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class ClusterProcessManager extends DefaultProcessManager {

    private final static Logger LOGGER = Logging.getLogger(ClusterExecutionStatus.class);

    /** The cluster id. */
    private String clusterId;

    /** The available storage. */
    private ProcessStorage processStorage;

    /** The list of excluded proces names. */
    private List<String> processNamesEsclusionList = new ArrayList<String>();

    private ClusterFilePublisherURLMangler mangler;

    private SendMail sendMail;

    private GeoServer geoserver;

    /**
     * Submit chained.
     * 
     * @param executionId the execution id
     * @param processName the process name
     * @param inputs the inputs
     * @return the map
     * @throws ProcessException the process exception
     */
    @Override
    public Map<String, Object> submitChained(String executionId, Name processName,
            Map<String, Object> inputs) throws ProcessException {
        // straight execution, no thread pooling, we're already running in the parent process thread
        final ProcessListener listener = new ProcessListener(new ExecutionStatus(processName,
                executionId, ProcessState.RUNNING, 0));
        final ProcessFactory pf = Processors.createProcessFactory(processName);
        if (pf == null) {
            throw new WPSException("No such process: " + processName);
        }

        // execute the process in the same thread as the caller
        final Process p = pf.create(processName);
        Map<String, Object> result = p.execute(inputs, listener);

        // analyse result
        if (listener.exception != null) {
            final boolean isProcessFilteredOut = processNamesEsclusionList.contains(processName
                    .getLocalPart());
            if (!isProcessFilteredOut) {
                // processStorage.putStatus( executionId, new ExecutionStatus(processName,executionId, ProcessState.FAILED, 100), false);
                // processStorage.storeResult( executionId, listener.exception.getMessage(),false);
            }
            throw new ProcessException("Process failed: " + listener.exception.getMessage(),
                    listener.exception);
        }
        return result;
    }

    /**
     * Submit.
     * 
     * @param executionId the execution id
     * @param processName the process name
     * @param inputs the inputs
     * @param background the background
     * @throws ProcessException the process exception
     */
    @Override
    public void submit(String executionId, Name processName, Map<String, Object> inputs,
            boolean background) throws ProcessException {

        // is this a process to NOT log?
        final boolean isProcessFiltered = (processNamesEsclusionList.contains(processName.getLocalPart()))||!background;

        final ExecutionStatusEx status = isProcessFiltered ? createExecutionStatus(processName,
                executionId) : new ClusterExecutionStatus(processName, clusterId, executionId,
                background, inputs);
        final ProcessListener listener = isProcessFiltered ? new ProcessListener(status)
                : new ClusterProcessListener((ClusterExecutionStatus) status);
        status.listener = listener;
        final ClusterProcessCallable callable = new ClusterProcessCallable(inputs, status, listener);
        Future<Map<String, Object>> future;
        if (background) {
            future = asynchService.submit(callable);
        } else {
            future = synchService.submit(callable);
        }

        status.future = future;
        executions.put(executionId, status);
    }

    class ClusterProcessListener extends ProcessListener {

        /**
         * @param status
         */
        public ClusterProcessListener(ClusterExecutionStatus status) {
            super(status);
        }


        @Override
        public void exceptionOccurred(Throwable exception) {
            super.exceptionOccurred(exception);

            // log the exception
            ((ClusterExecutionStatus) status).setException(exception);

        }

    }

    /**
     * The Class ClusterProcessCallable.
     */
    class ClusterProcessCallable implements Callable<Map<String, Object>> {

        /** The inputs. */
        Map<String, Object> inputs;

        /** The status. */
        ExecutionStatus status;

        /** The listener. */
        ProcessListener listener;

        /**
         * Instantiates a new cluster process callable.
         * 
         * @param inputs the inputs
         * @param status the status
         * @param listener the listener
         */
        public ClusterProcessCallable(Map<String, Object> inputs, ExecutionStatus status,
                ProcessListener listener) {
            this.inputs = inputs;
            this.status = status;
            this.listener = listener;
        }

        /**
         * Call.
         * 
         * @return the map
         * @throws Exception the exception
         */
        @Override
        public Map<String, Object> call() throws Exception {

            final Name processName = status.getProcessName();
            try {
                // start execution
                resourceManager.setCurrentExecutionId(status.getExecutionId());

                ProcessFactory pf = Processors.createProcessFactory(processName);
                if (pf == null) {
                    throw new WPSException("No such process: " + processName);
                }

                // execute the process
                Process p = pf.create(processName);
                if (p == null) {
                    throw new WPSException("Unabe to create process: " + processName);
                }

                // execute and get the output
                status.setPhase(ProcessState.RUNNING);
                Map<String, Object> result = p.execute(inputs, listener);
                String executionId = status.executionId;

                // analyze status
                if (listener.exception != null) {
                    // FAILED rethrow and then catch below
                    throw listener.exception;
                } else {
                    // SUCCESS

                    for (Entry<String, Object> entry : result.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase("result")) {
                            // move to WPS directory if needed
                            Object value = entry.getValue();
                            if (value instanceof File) {
                                final File outputFile = (File) value;

                                // target file
                                final File resultFile = resourceManager
                                        .getStoredResponseFile(executionId);
                                final String parentDir = resultFile.getParent();
                                final File targetFile = new File(parentDir,
                                        FilenameUtils.getBaseName(resultFile.getAbsolutePath())
                                                + ".zip");
                                if (!outputFile.getCanonicalPath().equals(
                                        targetFile.getCanonicalPath())) {
                                    // move while renaming
                                    FileUtils.moveFile(outputFile, targetFile);
                                    entry.setValue(targetFile);// replace value for this key
                                    value = targetFile;
                                }
                            }

                            // set real output
                            if (status instanceof ClusterExecutionStatus) {
                                ((ClusterExecutionStatus) status).setOutput(value);
                            }
                            break;
                        }
                    }

                }
                return result;
            } catch (Throwable e) {
                listener.exceptionOccurred(e);
                status.setPhase(ProcessState.FAILED);
                throw new WPSException("Process failed", e);
            } finally {
                // update status unless cancelled
                if (status.getPhase() == ProcessState.RUNNING) {
                    status.setPhase(ProcessState.COMPLETED);
                }
            }
        }

    }

    /**
     * The Class ClusterExecutionStatus.
     */
    class ClusterExecutionStatus extends ExecutionStatusEx {

        /** The cluster id. */
        private String clusterId;

        /** The background. */
        private boolean background;

        private Throwable exception;

        private Object output;

        private String baseURL;

        private int expirationDelay = -1;

        private ProcessDescriptor process;

        /**
         * Instantiates a new cluster execution status.
         * 
         * @param processName the process name
         * @param clusterId the cluster id
         * @param executionId the execution id
         * @param inputs
         */
        public ClusterExecutionStatus(Name processName, String clusterId, String executionId,
                boolean background, Map<String, Object> inputs) {
            super(processName, executionId);
            this.clusterId = clusterId;
            this.background = background;

            // create process
            this.process = new ProcessDescriptor();
            process.setClusterId(clusterId);
            process.setExecutionId(executionId);
            process.setEmail(extractEmail(inputs));
            process.setStartTime(new Date());
            process.setName(processName.getLocalPart());
            process.setNameSpace(processName.getNamespaceURI());
            process.setProgress(0.0f);
            process.setPhase(ProcessState.QUEUED);
            processStorage.create(process);

            // initialize default value for testing
            baseURL = "http://geoserver/fakeroot";
            if (Dispatcher.REQUEST.get() != null) {
                baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            }

            // handle the resource expiration timeout
            WPSInfo info = geoserver.getService(WPSInfo.class);
            double timeout = info.getResourceExpirationTimeout();
            if (timeout > 0) {
                expirationDelay = ((int) timeout * 1000);
            } else {
                // specified timeout == -1, so we use the default of five minutes
                expirationDelay = (5 * 60 * 1000);
            }
        }

        /**
         * @param inputs
         * @return
         */
        private String extractEmail(Map<String, Object> inputs) {
            if (inputs != null && !inputs.isEmpty()) {
                for (Entry<String, Object> entry : inputs.entrySet()) {
                    final String key = entry.getKey();
                    if (key.equalsIgnoreCase("email")) {
                        Object value = entry.getValue();
                        if (value != null && value instanceof String) {
                            return (String) value;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Gets the cluster id.
         * 
         * @return the cluster id
         */
        public String getClusterId() {
            return clusterId;
        }

        /**
         * Sets the phase.
         * 
         * @param phase the new phase
         */
        @Override
        public void setPhase(ProcessState phase) {
            try {

                // update super class
                super.setPhase(phase);

                // update phase
                process.setPhase(phase);

                if (phase == ProcessState.COMPLETED) {

                    // DO NOTHING we use the setOutput to signal the completion
                    return;
                    // super.setProgress(100.0f); // completed
                    // process.setProgress(100.f);
                    // // processStorage.putOutput( executionId, this, true);
                }

                final String email = process.getEmail();
                if (phase == ProcessState.RUNNING) {

                    // update
                    processStorage.update(process);

                    // email for running state
                    if (email != null) {
                        sendMail.sendStartedNotification(email, executionId);
                    }
                }

                if (phase == ProcessState.FAILED) {

                    super.setProgress(100.0f); // failed

                    // update
                    final String localizedMessage;
                    final Throwable cause = exception.getCause();
                    if (cause != null) {
                        localizedMessage = cause.getLocalizedMessage();
                    } else {
                        localizedMessage = exception.getLocalizedMessage();
                    }
                    processStorage.storeResult(process, localizedMessage);

                    // email
                    if (email != null) {
                        sendMail.sendFailedNotification(email, executionId, localizedMessage);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Gets the status.
         * 
         * @return the status
         */
        @Override
        public ExecutionStatus getStatus() {
            return this;
        }

        /**
         * Sets the progress.
         * 
         * @param progress the new progress
         */
        @Override
        public void setProgress(float progress) {
            super.setProgress(progress);
            process.setProgress(progress);
            processStorage.update(process);
        }

        /**
         * @param background the background to set
         */
        public void setBackground(boolean background) {
            this.background = background;
        }

        /**
         * @return the background
         */
        public boolean isBackground() {
            return background;
        }

        public void warningOccurred(String source, String location, String warning) {

        }

        /**
         * @return the exception
         */
        public Throwable getException() {
            return exception;
        }

        /**
         * @param exception the exception to set
         */
        public void setException(Throwable exception) {
            this.exception = exception;
        }

        /**
         * @return the output
         */
        public Object getOutput() {
            return output;
        }

        /**
         * @param output the output to set
         */
        public void setOutput(Object output) {
            this.output = output;
            final String email = process.getEmail();
            try {
                if (output instanceof File) {
                    final File file = (File) output;
                    final String publishingURL = mangler.getPublishingURL(file, baseURL);
                    processStorage.storeResult(process, publishingURL);
                    if (email != null) {
                        sendMail.sendFinishedNotification(email, executionId, publishingURL,
                                expirationDelay);
                    }
                } else {
                    processStorage.storeResult(process, output);
                    if (email != null) {
                        sendMail.sendFinishedNotification(email, executionId, output.toString(),
                                expirationDelay);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return the baseURL
         */
        public String getBaseURL() {
            return baseURL;
        }
    }

    /**
     * Instantiates a new cluster process manager using a list of excluded processes.
     * 
     * @param resourceManager
     * @param clusterid
     * @param clusterId
     * @param localProcesses
     * @param availableStorages
     */
    public ClusterProcessManager(GeoServer geoserver, WPSResourceManager resourceManager,
            ProcessStorage processStorage, List<String> processNamesEsclusionList,
            ClusterFilePublisherURLMangler urlMangler, SendMail sendMail, String clusterid) {
        super(resourceManager);

        // if no storage is available just initialize an empty list
        this.processStorage = processStorage;
        this.clusterId = clusterid;
        this.mangler = urlMangler;
        this.processNamesEsclusionList.addAll(processNamesEsclusionList);
        this.sendMail = sendMail;
        this.geoserver = geoserver;
    }

    public void init() {

        // look for zombies
        Collection<ProcessDescriptor> processes = processStorage.getAll(
                Arrays.asList(ProcessState.QUEUED, ProcessState.RUNNING), clusterId, null);

        // move them to failed sending an email
        for (ProcessDescriptor process : processes) {
            final String executionId = process.getExecutionId();
            final String localizedMessage = "Process has failed due to unknown reason";

            // change status to failed with result
            process.setPhase(ProcessState.FAILED);
            process.setProgress(100.f);
            processStorage.update(process);

            processStorage.storeResult(process, localizedMessage);

            // email
            String email = process.getEmail();
            if (email != null) {
                sendMail.sendFailedNotification(email, executionId, localizedMessage);
            }
        }
    }

    /**
     * Gets the cluster id.
     * 
     * @return the cluster id
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Gets the priority.
     * 
     * @return the priority
     */
    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}