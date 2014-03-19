/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Ows11Util;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.executor.DefaultProcessManager.ProcessListener;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.executor.storage.dao.BaseDAOTest;
import org.opengis.feature.type.Name;

import com.thoughtworks.xstream.XStream;

/**
 * Alternative implementation of ProcessManager, using a storage (ProcessStorage) to share process status between the instances of a cluster.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 * 
 */
public class ClusterProcessManagerTest extends WPSTestSupport {

    private static final boolean skipTest = false;

    @Override
    protected void setUpTestData(SystemTestData dataDirectory) throws Exception {
        super.setUpTestData(dataDirectory);

        addWcs11Coverages(dataDirectory);

        new File(dataDirectory.getDataDirectoryRoot(), "wps-cluster").mkdirs();
        dataDirectory.copyTo(
                BaseDAOTest.class.getClassLoader().getResourceAsStream(
                        "wps-cluster/dbConfig.properties"), "wps-cluster/dbConfig.properties");
        dataDirectory.copyTo(
                BaseDAOTest.class.getClassLoader().getResourceAsStream(
                        "wps-cluster/wpsCluster.properties"), "wps-cluster/wpsCluster.properties");
    }

    /**
     * Test serialization.
     * 
     * @throws Exception the exception
     */
    public void testSerialization() throws Exception {

        if (skipTest)
            return;

        ExecutionStatusExTest statusSrc = new ExecutionStatusExTest(Ows11Util.name("test_process"),
                "0");

        statusSrc.setProgress(99.9f);
        statusSrc.setPhase(ProcessState.RUNNING);

        Map<String, Object> testOutput = new HashMap<String, Object>();
        testOutput.put("id1", new Integer(1));
        testOutput.put("id2", new String("2"));
        testOutput.put("id3", new Double(3.0));
        MyClass myClass = new MyClass();
        myClass.setValue(new BigDecimal(4));
        testOutput.put("id4", myClass);

        statusSrc.setOutput(testOutput);

        XStream xstream = new XStream();

        String marshalled = xstream.toXML(statusSrc);

        ExecutionStatus statusTrg = (ExecutionStatus) xstream.fromXML(marshalled);

        assertEquals(statusTrg.getExecutionId(), statusSrc.getExecutionId());
        assertEquals(statusTrg.getProcessName().getLocalPart(), statusSrc.getProcessName()
                .getLocalPart());

        assertEquals(statusTrg.getProgress(), statusSrc.getProgress());
        assertEquals(statusTrg.getPhase(), statusSrc.getPhase());

        Map<String, Object> trgOutput = statusTrg.getOutput(0);

        for (Entry<String, Object> entry : trgOutput.entrySet()) {
            if (entry.getValue() instanceof MyClass) {
                assertEquals(((MyClass) entry.getValue()).getValue(),
                        ((MyClass) testOutput.get(entry.getKey())).getValue());
            } else {
                assertEquals(entry.getValue(), testOutput.get(entry.getKey()));
            }
        }
    }

    /**
     * The Class MyClass.
     */
    class MyClass {

        /** The value. */
        private BigDecimal value;

        /**
         * Sets the value.
         * 
         * @param value the new value
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }

        /**
         * Gets the value.
         * 
         * @return the value
         */
        public BigDecimal getValue() {
            return value;
        }
    }

    /**
     * A pimped up test execution status.
     * 
     * @author Alessio Fabiani - GeoSolutions
     */
    static class ExecutionStatusExTest extends ExecutionStatus {

        /** The output. */
        private Map<String, Object> output;

        /** The listener. */
        ProcessListener listener;

        /**
         * Instantiates a new execution status ex test.
         * 
         * @param processName the process name
         * @param executionId the execution id
         */
        public ExecutionStatusExTest(Name processName, String executionId) {
            super(processName, executionId, ProcessState.QUEUED, 0);
        }

        /**
         * Gets the status.
         * 
         * @return the status
         */
        public ExecutionStatus getStatus() {
            return new ExecutionStatus(processName, executionId, phase, progress);
        }

        /**
         * Sets the phase.
         * 
         * @param phase the new phase
         */
        @Override
        public void setPhase(ProcessState phase) {
            super.setPhase(phase);

        }

        /**
         * Gets the output.
         * 
         * @param timeout the timeout
         * @return the output
         * @throws Exception the exception
         */
        @Override
        public Map<String, Object> getOutput(long timeout) throws Exception {
            if (output == null)
                throw new Exception("Null output!");
            return output;
        }

        /**
         * Sets the output.
         * 
         * @param output the output
         */
        public void setOutput(Map<String, Object> output) {
            this.output = output;
        }

    }
}