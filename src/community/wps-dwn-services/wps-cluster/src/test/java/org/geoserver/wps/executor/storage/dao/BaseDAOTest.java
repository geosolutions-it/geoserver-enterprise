/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.wps.executor.storage.dao;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;

/**
 * The Class BaseDAOTest.
 * 
 * @author Alessio Fabiani <alessio.fabiani at geo-solutions.it>
 */
public abstract class BaseDAOTest extends GeoServerTestSupport {

    /** The logger. */
    protected final Logger LOGGER = Logging.getLogger(BaseDAOTest.class);

    /** The marshaller. */
    private XStream marshaller = new XStream();

    /** The process dao. */
    protected static ProcessDescriptorDAO processDAO;

    /**
     * Populate data directory.
     * 
     * @param dataDirectory the data directory
     * @throws Exception the exception
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        dataDirectory.addWcs10Coverages();

        new File(dataDirectory.getDataDirectoryRoot(), "wps-cluster").mkdirs();
        dataDirectory.copyTo(
                BaseDAOTest.class.getClassLoader().getResourceAsStream(
                        "wps-cluster/dbConfig.properties"), "wps-cluster/dbConfig.properties");
        dataDirectory.copyTo(
                BaseDAOTest.class.getClassLoader().getResourceAsStream(
                        "wps-cluster/wpsCluster.properties"), "wps-cluster/wpsCluster.properties");
    }

    /**
     * Sets the up internal.
     * 
     * @throws Exception the exception
     */
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        processDAO = (ProcessDescriptorDAO) applicationContext.getBean("processStorageDAO");
        removeAll();
    }

    /**
     * Test check da os.
     */
    public void testCheckDAOs() {
        assertNotNull(processDAO);
    }

    /**
     * Removes the all.
     */
    protected void removeAll() {
        removeAllProcesses();
    }

    /**
     * Removes the all processes.
     */
    protected void removeAllProcesses() {
        List<ProcessDescriptor> list = processDAO.findAll();
        for (ProcessDescriptor item : list) {
            LOGGER.info("Removing " + item);
            boolean ret = processDAO.remove(item);
            assertTrue("ProcessDescriptor not removed", ret);
        }

        assertEquals("ProcessDescriptors have not been properly deleted", 0, processDAO.count(null));
    }

    /**
     * Creates the process descriptor.
     * 
     * @param clusterId the cluster id
     * @param executionId the execution id
     * @return the process descriptor
     */
    protected ProcessDescriptor createProcessDescriptor(String clusterId, String executionId) {
        ProcessDescriptor process = new ProcessDescriptor();
        process.setClusterId(clusterId);
        process.setExecutionId(executionId);
        // process.setStatus(marshaller.toXML(createStatus(clusterId, executionId,
        // ProcessState.QUEUED, 0.0f)));
        process.setName("");
        process.setNameSpace("");
        process.setProgress(0.0f);
        process.setPhase(ProcessState.QUEUED);
        process.setStartTime(new Date());

        return process;
    }

    // /**
    // * Creates the status.
    // *
    // * @param clusterId the cluster id
    // * @param executionId the execution id
    // * @param phase the phase
    // * @param progress the progress
    // * @return the object
    // */
    // private Object createStatus(String clusterId, String executionId, ProcessState phase,
    // float progress) {
    // return new ExecutionStatus(Ows11Util.name(clusterId), executionId, phase, progress);
    // }

}
