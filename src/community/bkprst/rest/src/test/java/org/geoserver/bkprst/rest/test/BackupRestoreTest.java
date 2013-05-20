/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.rest.test;

import java.io.File;

import org.restlet.data.Status;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class BackupRestoreTest extends BaseBackupRestoreTest {

    public void testBackupRestoreNoDataNoGwcNoLog() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        final File dir= new File(backupDir);       
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        String outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200&& outputStreamContent.contains("<id>"));
        

        // wait for completion
        // Query the just added task
        String taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);
        
        
        // the three txt files should not be there, but we have a backup.txt file added
        assertEquals(Utils.getNumFiles(this.dataRoot.root()) - 3 + 1, Utils.getNumFiles(dir));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);
        taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);

        assertEquals(Utils.getNumFiles(this.dataRoot.root()) - 3, Utils.getNumFiles(dir));
    }

    public void testBackupRestoreDataNoGwcNoLog() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        final File dir= new File(backupDir);       
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        String outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200&& outputStreamContent.contains("<id>"));
        

        // wait for completion
        // Query the just added task
        String taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);
        
        // the three txt files should not be there, but we have a backup.txt file added
        assertEquals(Utils.getNumFiles(this.dataRoot.root()) - 2 + 1, Utils.getNumFiles(dir));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);
        taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);

        assertEquals(Utils.getNumFiles(this.dataRoot.root()) - 2, Utils.getNumFiles(dir));        
    }

    public void testBackupRestoreDataGwcNoLog() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        final File dir= new File(backupDir);       
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>true</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        String outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200&& outputStreamContent.contains("<id>"));
        

        // wait for completion
        // Query the just added task
        String taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);
        
        // the three txt files should not be there, but we have a backup.txt file added
        assertEquals(Utils.getNumFiles(this.dataRoot.root()) - 1 + 1, Utils.getNumFiles(dir));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        outputStreamContent = response.getOutputStreamContent();
        assertTrue(response.getStatusCode() == 201 && response.getErrorCode() == 200);
        taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);

        assertEquals(Utils.getNumFiles(this.dataRoot.root()) -1, Utils.getNumFiles(dir));        
    }

    public void testBackupRestoreDataGwcLog() throws Exception {
       String backupDir=Utils.prepareBackupDir(this); 
       final File dir= new File(backupDir);
       MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "<includedata>true</includedata>" 
                + "<includegwc>true</includegwc>"
                + "<includelog>true</includelog>" 
                + "</task>");
        String outputStreamContent = response.getOutputStreamContent();
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        assertTrue(outputStreamContent.contains("<id>"));
        

        // wait for completion
        // Query the just added task
        String taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);
        
        // the three txt files should not be there, but we have a backup.xml file added
        assertEquals(Utils.getNumFiles(this.dataRoot.root())+ 1, Utils.getNumFiles(dir));
        
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>" + backupDir + "</path>"
                + "</task>");
        outputStreamContent = response.getOutputStreamContent();
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  
        taskID=Utils.parseTaskID(outputStreamContent);
        waitForTaskCompletion(taskID);

        assertEquals(Utils.getNumFiles(this.dataRoot.root()), Utils.getNumFiles(dir));          
    }

    public void testAddingTasks() throws Exception {
        String backupDir=Utils.prepareBackupDir(this);
        // No tasks should be present
        MockHttpServletResponse response = getAsServletResponse("/rest/bkprst");
        assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());           
        assertTrue(response.getOutputStreamContent().contains("<tasks/>"));
        
        // Adding of a backup task
        response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>"+backupDir+"</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());   
        assertTrue(response.getOutputStreamContent().contains("<id>"));
    
        // Adding of a restore task while the backup is in progress
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>"+backupDir+"</path>"
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  
    
        // Adding of a restore task after the backup is completed
        Thread.sleep(6000); 
        response = postAsServletResponse("/rest/bkprst/restore", 
                "<task>" 
                + "<path>"+backupDir+"</path>"
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        assertTrue(response.getOutputStreamContent().contains("<id>"));
    
        // Some tasks sould be present
        response = getAsServletResponse("/rest/bkprst");
        assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        assertTrue(response.getOutputStreamContent().contains("<restore>") && response.getOutputStreamContent().contains("<backup>"));
    }

    public void testQueryTask() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        // Adding of a backup task
        MockHttpServletResponse response = postAsServletResponse("/rest/bkprst/backup", 
                "<task>" 
                + "<path>/tmp/"+backupDir+"</path>"
                + "<includedata>false</includedata>" 
                + "<includegwc>false</includegwc>"
                + "<includelog>false</includelog>" 
                + "</task>");
        assertEquals(Status.SUCCESS_CREATED.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        assertTrue(response.getOutputStreamContent().contains("<id>"));
        String id= response.getOutputStreamContent().substring(4, response.getOutputStreamContent().length() - 5);
        
        // Query the just added task
        response = getAsServletResponse("/rest/bkprst/" + id);
        assertEquals(Status.SUCCESS_OK.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());          
        assertTrue(response.getOutputStreamContent().contains("<backup>"));
        
        // Query a non-esisting task
        response = getAsServletResponse("/rest/bkprst/" + id + "123");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND.getCode(),response.getStatusCode()); 
        assertEquals(Status.SUCCESS_OK.getCode(),response.getErrorCode());  
    }
}
