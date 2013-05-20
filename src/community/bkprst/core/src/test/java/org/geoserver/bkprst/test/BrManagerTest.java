/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.bkprst.BrManager;
import org.geoserver.bkprst.TaskNotFoundException;
import org.geoserver.bkprst.UnallowedOperationException;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.test.GeoServerAbstractTestSupport;

/**
 * Test the BR tool task manager
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BrManagerTest extends CatalogRESTTestSupport {

    private BrManager br;

    public static int TASKDURATION = 5000;

    public void setUpInternal() {
        this.br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
    }

    public void testUUIDRestore() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addRestoreTask(backupDir);
        assertNotNull(id);
    }

    public void testUUIDBackup() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addBackupTask(backupDir, false, false, false);
        assertNotNull(id);
    }

    public void testStopBackup1() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addBackupTask(backupDir, false, false, false);
        try {
            this.br.stopBackupTask(id);
        } catch (UnallowedOperationException e) {
            assertTrue(false);
            return;
        } catch (TaskNotFoundException e) {
            assertTrue(false);
            return;
        }
        assertTrue(true);
    }

    public void testStopBackup2() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        this.br.addBackupTask(backupDir, false, false, false);
        try {
            this.br.stopBackupTask(UUID.randomUUID());
        } catch (UnallowedOperationException e) {
            assertTrue(false);
            return;
        } catch (TaskNotFoundException e) {
            assertTrue(true);
            return;
        }
        assertTrue(false);
    }

    public void testStopRestore() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addRestoreTask(backupDir);
        try {
            this.br.stopBackupTask(id);
        } catch (UnallowedOperationException e) {
            assertTrue(true);
            return;
        } catch (TaskNotFoundException e) {
            assertTrue(false);
            return;
        }
        assertTrue(false);
    }

    public void testCleanup() throws Exception {
        String backupDir=Utils.prepareBackupDir(this); 
        this.br.addTask(new MockRestoreTask(br.generateId(), backupDir, br.getWriteLocker()));
        this.br.addTask(new MockBackupTask(br.generateId(), backupDir, br.getWriteLocker()));
        this.br.addTask(new MockRestoreTask(br.generateId(), backupDir, br.getWriteLocker()));
        assertEquals(3, this.br.getAllTasks().size());

        try {
            // NOTE: 60000 depends on the task retention time value set in the Spring configuration
            Thread.sleep((BrManagerTest.TASKDURATION + 1000) * 4 + 60000);  
        } catch (InterruptedException e) {
            assertTrue(false);
        }

        this.br.cleanupTasks();
        assertEquals(0, this.br.getAllTasks().size());
    }

}
