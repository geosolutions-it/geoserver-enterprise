/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.geoserver.bkprst.BrManager;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class TransactionTest extends GeoServerTestSupport {

    public GeoServerDataDirectory dataRoot;
    public String backupDir= "/tmp/testBackup1";

    public void setUpInternal() {
        this.dataRoot = this.getDataDirectory();
        try {
            this.populateDataDirectory(getTestData());
        } catch (Exception e) {
            LOGGER.fine(e.getMessage());
        }
        
        BrManager br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
        br.setTest();
    }
    
    /**
     * Populates a mock data directory with standard data
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
        File mockDir= dataDirectory.getDataDirectoryRoot();
        
        File logs = new File(mockDir + "/logs");
        logs.mkdirs();
        new File(logs, "log.txt").createNewFile();
        File gwc = new File(mockDir + "/gwc");
        gwc.mkdirs();
        new File(gwc, "gwc.txt").createNewFile();
        File data = new File(mockDir + "/data");
        data.mkdirs();
        new File(data, "data.txt").createNewFile();
    }

    protected int getNumFiles(File dir) {
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).size();
    }
    
    public void testBackupDelete() throws Exception {
        MockHttpServletResponse response;
        
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        Thread.sleep(1000);
        assertTrue((new File(this.backupDir)).exists());

        // Deletes on-oging backup task
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + id);
        Thread.sleep(1000);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        Thread.sleep(1000);
        
        // Checks status of deleted task
        response= this.getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        assertTrue(response.getOutputStreamContent().contains("STOPPED"));
        
        // Checks the backup dir has been deleted
        Thread.sleep(1000);
        assertFalse((new File(this.backupDir)).exists());
        
        // Checks non numeric ID
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + id + "xxx");
        assertTrue(response.getStatusCode() == 404 && response.getErrorCode() == 200);

        // Checks non-existing ID
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + id + "123");
        assertTrue(response.getStatusCode() == 404 && response.getErrorCode() == 200);
    }
    
    public void testRestoreDelete() throws Exception {
        MockHttpServletResponse response;
        int nFiles= this.getNumFiles(this.dataRoot.root());

        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        Thread.sleep(BrManager.TESTTIME + 1000);
        
        // Checks status of backup task
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        response= this.getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        assertTrue(response.getOutputStreamContent().contains("COMPLETED"));

        // Starts restore task
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        Thread.sleep(1000);

        // Checks status of restore task
        id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        response= this.getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        assertTrue(response.getOutputStreamContent().contains("RUNNING"));

        // Deletes on-oging restore task
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        Thread.sleep(1000);
        
        // Checks status of deleted task
        response= this.getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200);
        assertTrue(response.getOutputStreamContent().contains("STOPPED"));
        
        // Checks the data dir has the same number of files than before restore
        assertEquals(this.getNumFiles(this.dataRoot.root()), nFiles);
        
        // Checks non numeric ID
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + id + "xxx");
        assertTrue(response.getStatusCode() == 404 && response.getErrorCode() == 200);

        // Checks non-existing ID
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + id + "123");
        assertTrue(response.getStatusCode() == 404 && response.getErrorCode() == 200);
    }
 
}