/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class BrManagerResourceTest extends GeoServerTestSupport {

    public GeoServerDataDirectory dataRoot;

    public void setUpInternal() {
        this.dataRoot = this.getDataDirectory();
    }
    
    /**
     * Populates a mock data directory with standard data
     */
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
        File mockDir= dataDirectory.getDataDirectoryRoot();
        String srcDir= mockDir.getAbsolutePath() + "/../../src/test/data";
        
        FileUtils.copyDirectoryToDirectory(new File(srcDir + "/logs"), mockDir);
        FileUtils.copyDirectoryToDirectory(new File(srcDir + "/gwc"), mockDir);
        FileUtils.copyDirectoryToDirectory(new File(srcDir + "/data"), mockDir);
    }

    public void testAddingTasks() throws Exception {
        MockHttpServletResponse response;
        
        // No tasks should be present
        response = getAsServletResponse("/rest/bkprst");
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<tasks/>"));
        
        // Adding of a backup task
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
 
        // Adding of a restore task while the backup is in progress
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "</task>");
        
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);

        // Adding of a restore task after the backup is completed
        Thread.sleep(6000); 
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));

        // Some tasks sould be present
        response = getAsServletResponse("/rest/bkprst");
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<restore>") && response.getOutputStreamContent().contains("<backup>"));
    }
    
    public void testQueryTask() throws Exception {
        MockHttpServletResponse response;
               
        // Adding of a backup task
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        
        // Query the just added task
        response = getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<backup>"));
        
        // Query a non-esisting task
        response = getAsServletResponse("/rest/bkprst/" + id + "123");
        assertTrue(response.getStatusCode() == 404 && response.getErrorCode() == 200);
    }
    
    public void testBackup() throws Exception {
        MockHttpServletResponse response;
        
        // Adding of a backup task
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        
        // Query the just added task
        response = getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<backup>"));
    }

    public void testRestore() throws Exception {
        MockHttpServletResponse response;
        
        // Adding of a restore task
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>/tmp/testBackup1</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        
        // Query the just added task
        response = getAsServletResponse("/rest/bkprst/" + id);
        assertTrue(response.getStatusCode() == 200 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<restore>"));
    }
    
}
