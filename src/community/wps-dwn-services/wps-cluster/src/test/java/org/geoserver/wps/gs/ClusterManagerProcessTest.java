/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.output.WriterOutputStream;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.storage.ProcessStorage;
import org.geoserver.wps.executor.storage.TestProcessStorage;
import org.geoserver.wps.executor.storage.dao.BaseDAOTest;
import org.geoserver.wps.ppio.ExecutionStatusPPIO;
import org.geotools.util.NullProgressListener;

/**
 * The Class ClusterManagerProcessTest.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class ClusterManagerProcessTest extends GeoServerTestSupport {

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
     * PPIO Test.
     * 
     * @throws Exception the exception
     */
    public void testEncodeStatus() throws Exception {
        
        TestProcessStorage testProcessStorage = (TestProcessStorage) applicationContext.getBean("testClusteredProcessStorage");
        testProcessStorage.setTestMode(true);
        
        ProcessStorage storage = null;
        List<ProcessStorage> availableStorages = GeoServerExtensions
                .extensions(ProcessStorage.class);
        if (availableStorages == null || availableStorages.size() == 0) {
            throw new RuntimeException("No available Process Storage registered on GeoServer!");
        }
        storage = availableStorages.get(0);

        final String executionId = UUID.randomUUID().toString();
        ExecutionStatus status = new ClusterManagerProcess(storage).execute(executionId,
                new NullProgressListener() // progressListener
                );

        assertNotNull(status);

        ExecutionStatusPPIO ppio = new ExecutionStatusPPIO(getGeoServer(), null);
        StringWriter writer = new StringWriter();
        ppio.encode(status, new WriterOutputStream(writer));

        String statusList = writer.toString();

        assertNotNull(statusList);

        Object outputStatus = ppio.decode(statusList);

        try {
            ExecutionStatus output = (ExecutionStatus) outputStatus;

            assertNotNull(outputStatus);
            assertEquals(status.getExecutionId(), output.getExecutionId());
            assertEquals(status.getProcessName(), output.getProcessName());
            assertEquals(status.getPhase(), output.getPhase());
            assertEquals(status.getProgress(), output.getProgress());
        } catch (Exception e) {
            assertFalse(true);
        }
    }
}
