/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.test;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.bkprst.BrManager;
import org.geoserver.bkprst.BrTask;
import org.geoserver.bkprst.BrTaskState;
import org.geoserver.bkprst.ConfigurableDispatcherCallback;
import org.geoserver.bkprst.TaskNotFoundException;
import org.geoserver.bkprst.UnallowedOperationException;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerAbstractTestSupport;

/**
 * Test the BR tool task manager
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BrManagerTest extends CatalogRESTTestSupport {

    private BrManager br;

    public static int TASKDURATION = 5000;
    
    public static String path= "/tmp/aaa"; // FIXZME: 

    public void setUpInternal() {
        this.br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
    }

    public void testUUIDRestore() {
        UUID id = this.br.addRestoreTask(BrManagerTest.path);
        assertNotNull(id);
    }

    public void testUUIDBackup() {
        UUID id = this.br.addBackupTask(BrManagerTest.path, false, false, false);
        assertNotNull(id);
    }

    public void testStopBackup1() {
        UUID id = this.br.addBackupTask(BrManagerTest.path, false, false, false);
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

    public void testStopBackup2() {
        this.br.addBackupTask(BrManagerTest.path, false, false, false);
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

    public void testStopRestore() {
        UUID id = this.br.addRestoreTask(BrManagerTest.path);
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

    public void testCleanup() {
        this.br.addTask(new MockRestoreTask(br.generateId(), BrManagerTest.path, br.getWriteLocker()));
        this.br.addTask(new MockBackupTask(br.generateId(), BrManagerTest.path, br.getWriteLocker()));
        this.br.addTask(new MockRestoreTask(br.generateId(), BrManagerTest.path, br.getWriteLocker()));
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
