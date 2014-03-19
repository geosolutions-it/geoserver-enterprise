/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.storage.ProcessStorage;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * The Class ClusterManagerProcess.
 * 
 * <p>
 * In the future this process could be used to stop ongoing processes. For the time being it simply access the executiong logs
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
@DescribeProcess(title = "Enterprise Cluster-Manager Process", description = "Allows to retrieve the Execution Status of the Cluster running processes.")
public class ClusterManagerProcess implements GSProcess {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(ClusterManagerProcess.class);
    
    /**
     * Special implementation of ExecutionStatus with support for encoding the result.
     * 
     * @author Simone Giannecchini, GeoSolutions SAS
     *
     */
    public final static class ExecutionStatusExt extends ExecutionStatus{
        /**
         * @param processName
         * @param executionId
         * @param phase
         * @param progress
         * @param result
         */
        public ExecutionStatusExt(Name processName, String executionId, ProcessState phase,
                float progress, String result) {
            super(processName, executionId, phase, progress);
            this.result = result;
        }

        /**The result of the processing, this is either a link to a zip file or an exception message.*/
        private final String result;

        /**
         * @return the result
         */
        public String getResult() {
            return result;
        }
        
        
    }

    private final ProcessStorage storage;

    /**
     * Instantiates a new cluster manager process.
     * 
     * @param storage the {@link ProcessStorage} to inquiry
     */
    public ClusterManagerProcess(ProcessStorage storage) {
        this.storage = storage;
        if (storage == null) {
            throw new RuntimeException("Provided null ProcessStorage");
        }
    }

    /**
     * Execute.
     * 
     * @param executionId the execution id
     * @param progressListener the progress listener
     * @return the list
     * @throws ProcessException the process exception
     */
    @DescribeResult(name = "result", description = "Zipped output files to download")
    public ExecutionStatus execute(
            @DescribeParameter(name = "executionId", min = 1, description = "The requested WPS ExecutionId") String executionId,
            ProgressListener progressListener) throws ProcessException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Requested status for execution ID: " + executionId);
        }
        ProcessDescriptor process = storage.findByExecutionId(executionId, true);
        if (process != null) {
            return new ExecutionStatusExt(new NameImpl(process.getNameSpace(), process.getName()),
                    process.getExecutionId(), process.getPhase(), process.getProgress(),process.getResult());
        }
        throw new ProcessException("Unable to find process with executionId: " + executionId);

    }

}