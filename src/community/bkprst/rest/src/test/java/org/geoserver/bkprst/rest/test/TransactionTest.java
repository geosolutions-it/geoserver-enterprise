/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.restlet.data.Status;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class TransactionTest extends BaseBackupRestoreTest {


    public void testBackupDelete() throws Exception {
        String backupDir=Utils.prepareBackupDir(this);
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        String outputStreamContent = response.getOutputStreamContent();
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());        
        assertTrue(outputStreamContent.contains("<id>"));
        final String taskID=Utils.parseTaskID(outputStreamContent);

        // Deletes ongoing backup task
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + taskID);
        assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());
        waitForTaskCompletion(taskID);
        
        // Checks the backup dir has been deleted
        assertFalse((new File(backupDir)).exists());
        
        // Checks non numeric ID
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + taskID + "xxx");
        assertEquals(Status.SERVER_ERROR_INTERNAL.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());

        // Checks non-existing taskID
        response= this.deleteAsServletResponse("/rest/bkprst/backup/" + taskID + "123");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());
    }
    
    public void testRestoreDelete() throws Exception {
        
        // start a backup
        String backupDir=Utils.prepareBackupDir(this);
        final int numFilesOriginal = Utils.getNumFiles(this.dataRoot.root());
        
        // start a backup and wait for completion
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        String outputStreamContent = response.getOutputStreamContent();
        String taskID=Utils.parseTaskID(outputStreamContent);
        assertNotNull(taskID);
        waitForTaskCompletion(taskID);

        // Checks the data dir has the same number of files than before restore
        assertEquals(numFilesOriginal-3+1, Utils.getNumFiles(new File(backupDir)));//backup.xml
        
        // Starts restore task and then stop it
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        outputStreamContent = response.getOutputStreamContent();
        taskID=Utils.parseTaskID(outputStreamContent);
        assertNotNull(taskID);

        // Deletes ongoing restore task
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + taskID);
        assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  
        
        // Checks status of deleted task
        response= this.getAsServletResponse("/rest/bkprst/" +taskID);
        if(Status.SUCCESS_OK.getCode()==response.getStatusCode()){
            assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
            assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());
            outputStreamContent = response.getOutputStreamContent();
            waitForTaskStatus(taskID,"STOPPED");            
        } else {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND.getCode(),response.getStatusCode()); 
            assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());
        }

        
        
        // Checks non numeric ID
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + taskID + "xxx");
        assertEquals(Status.SERVER_ERROR_INTERNAL.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  

        // Checks non-existing ID
        response= this.deleteAsServletResponse("/rest/bkprst/restore/" + taskID + "123");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  
        
        // clean up
        FileUtils.deleteDirectory(new File(backupDir));
    }
}