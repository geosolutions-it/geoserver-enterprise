/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst.test;

import java.util.UUID;

import org.geoserver.bkprst.BrManager;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.geoserver.web.GeoServerWicketTestSupport;

/**
 * Test the BR tool locks for Wicket (GUI) calls
 * 
 * @author Luca Morandini lmorandini@ieee.org
 */
public class BrManagerWicketTest extends GeoServerWicketTestSupport {

    private BrManager br;

    public void setUpInternal() throws Exception {
        super.setUpInternal();
        this.br = (BrManager) GeoServerAbstractTestSupport.applicationContext.getBean("brmanager");
    }

    public void testLockBackup() throws Exception {

        // Requests not blocked prior to adding a BR task
        login();
        tester.startPage(new org.geoserver.web.GeoServerHomePage());
        assertFalse(tester.getLastRenderedPage().isErrorPage());

        // Blocked requests after adding a Backup task
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addTask(new MockBackupTask(br.generateId(), backupDir, br.getWriteLocker()));
        Thread.sleep(2000);

        // Wicket requests blocked
        try {
            login();
            tester.startPage(new org.geoserver.web.GeoServerHomePage());
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
        
        // Waits for task to complete
        Thread.sleep(BrManagerTest.TASKDURATION + 1000);
        
        // Requests not blocked after task completion
        login();
        tester.startPage(new org.geoserver.web.GeoServerHomePage());
        assertFalse(tester.getLastRenderedPage().isErrorPage());
    }

    public void testLockRestore() throws Exception {
        
        // Not blocked prior to adding a BR task
        login();
        tester.startPage(new org.geoserver.web.GeoServerHomePage());
        assertFalse(tester.getLastRenderedPage().isErrorPage());

        // Blocked requests after adding a Restore task
        String backupDir=Utils.prepareBackupDir(this); 
        UUID id = this.br.addTask(new MockRestoreTask(br.generateId(), backupDir, br.getWriteLocker()));
        Thread.sleep(2000);

        // Wicket requests blocked
        try {
            login();
            tester.startPage(new org.geoserver.web.GeoServerHomePage());
            assertTrue(false);
        } catch (RuntimeException e) {
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }

        // Waits for task to complete
        Thread.sleep(BrManagerTest.TASKDURATION + 1000);
        
        // Requests not blocked after task completion
        login();
        tester.startPage(new org.geoserver.web.GeoServerHomePage());
        assertFalse(tester.getLastRenderedPage().isErrorPage());
    }

}
