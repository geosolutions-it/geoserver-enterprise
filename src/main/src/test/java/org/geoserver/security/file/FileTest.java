/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.file;


import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.impl.AbstractUserGroupService;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author christian
 *
 */
public  class FileTest extends TestCase {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    int gaCounter=0,ugCounter=0;
    
    GeoServerRoleService gaService = new AbstractRoleService() {
        
        public String getName() {
            return "TestGAService";
        };
        @Override
        protected void deserialize() throws IOException {
            gaCounter++;
        }

        @Override
        public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
            super.initializeFromConfig(config);
        }
    };
    
    GeoServerUserGroupService ugService = new AbstractUserGroupService() {
        
        public String getName() {
            return "TestUGService";
        };
        
        @Override
        protected void deserialize() throws IOException {
            ugCounter++;
        }

        @Override
        public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        }
    };
      
    public void testFileWatcher() {
        try {
            File ugFile = File.createTempFile("users", ".xml");
            ugFile.deleteOnExit();
            File gaFile = File.createTempFile("roles", ".xml");
            gaFile.deleteOnExit();
            
            RoleFileWatcher gaWatcher = new
                RoleFileWatcher(gaFile.getCanonicalPath(),gaService);
            assertEquals(1, gaCounter);
            
            gaWatcher.setDelay(10); // 10 millisecs
            gaWatcher.start();
            
            UserGroupFileWatcher ugWatcher = new           
                UserGroupFileWatcher(ugFile.getCanonicalPath(),ugService);
            assertEquals(1, ugCounter);
            ugWatcher.setDelay(10);
            ugWatcher.start();
            
            LOGGER.info(gaWatcher.toString());
            LOGGER.info(ugWatcher.toString());
            
            // now, modifiy last access
            ugFile.setLastModified(ugFile.lastModified()+1000);
            gaFile.setLastModified(gaFile.lastModified()+1000);
           
            // Try for one second
            int maxTries=10;
            boolean failed = true;
            for (int i=0; i <maxTries;i++) {
                if (ugCounter==2 && gaCounter==2) {
                    failed=false;
                    break;
                }
                Thread.sleep(100);
            }
            if (failed) {
                Assert.fail("FileWatchers not working" );
            }
            ugWatcher.setTerminate(true);
            gaWatcher.setTerminate(true);
            ugFile.delete();
            gaFile.delete();
            
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }        
    }
    
    
    
    public void testLockFile() {
        
        try {
            File fileToLock = File.createTempFile("test", ".xml");
            fileToLock.deleteOnExit();
            
            LockFile lf1 = new LockFile(fileToLock);
            LockFile lf2 = new LockFile(fileToLock);
        
            assertFalse(lf1.hasWriteLock());
            assertFalse(lf1.hasForeignWriteLock());
            
            lf2.writeLock();
            
            assertFalse(lf1.hasWriteLock());
            assertTrue(lf1.hasForeignWriteLock());            
            assertTrue(lf2.hasWriteLock());
            assertFalse(lf2.hasForeignWriteLock());
            
            lf2.writeUnLock();
            
            assertFalse(lf1.hasWriteLock());
            assertFalse(lf1.hasForeignWriteLock());            
            assertFalse(lf2.hasWriteLock());
            assertFalse(lf2.hasForeignWriteLock());
            
            lf2.writeLock();
            
            boolean fail = true;
            try {
                lf1.writeLock();
            } catch (IOException ex) {
                fail = false;
                LOGGER.info(ex.getMessage());
            }
            if (fail) {
                Assert.fail("IOException not thrown for concurrent write lock" );
            }
                
            lf2.writeUnLock();
            lf1.writeLock();

            assertTrue(lf1.hasWriteLock());
            assertFalse(lf1.hasForeignWriteLock());            
            assertFalse(lf2.hasWriteLock());
            assertTrue(lf2.hasForeignWriteLock());
            
            lf1.finalize();

            assertFalse(lf1.hasWriteLock());
            assertFalse(lf1.hasForeignWriteLock());            
            assertFalse(lf2.hasWriteLock());
            assertFalse(lf2.hasForeignWriteLock());

            
        } catch (Throwable ex) {
            Assert.fail(ex.getMessage());
        }                
    }    
}