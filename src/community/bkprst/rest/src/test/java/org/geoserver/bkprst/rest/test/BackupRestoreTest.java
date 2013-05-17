/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class BackupRestoreTest extends GeoServerTestSupport {

    public GeoServerDataDirectory dataRoot;
    public String backupDir= "/tmp/testBackup1";

    public void setUpInternal() {
        this.dataRoot = this.getDataDirectory();
        try {
            this.populateDataDirectory(getTestData());
        } catch (Exception e) {
            LOGGER.fine(e.getMessage());
        }
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

    public int getNumFiles(File dir) {
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).size();
    }
    
    public void testBackupRestoreNoDataNoGwcNoLog() throws Exception {
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

        Thread.sleep(6000);
        // the three txt files should not be there, but we have a backup.txt file added
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 3 + 1, this.getNumFiles(new File(backupDir)));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 3, this.getNumFiles(new File(backupDir)));
    }
    
    public void testBackupRestoreDataNoGwcNoLog() throws Exception {
        MockHttpServletResponse response;
        
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));

        Thread.sleep(6000);
        // there are at least 3 files we did not back up
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 2 + 1, this.getNumFiles(new File(backupDir)));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 2, this.getNumFiles(new File(backupDir)));
    }

    public void testBackupRestoreDataGwcNoLog() throws Exception {
        MockHttpServletResponse response;
        
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>true</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 1 + 1, this.getNumFiles(new File(backupDir)));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()) - 1, this.getNumFiles(new File(backupDir)));
    }

    public void testBackupRestoreDataGwcLog() throws Exception {
        MockHttpServletResponse response;
        
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>true</includegwc>"
                + "<includelog>true</includelog>" 
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200
                && response.getOutputStreamContent().contains("<id>"));

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()) + 1, this.getNumFiles(new File(backupDir)));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);

        Thread.sleep(6000);
        assertEquals(this.getNumFiles(this.dataRoot.root()), this.getNumFiles(new File(backupDir)));
    }
}
