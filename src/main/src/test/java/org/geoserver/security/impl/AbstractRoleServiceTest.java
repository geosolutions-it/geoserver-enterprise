/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */


package org.geoserver.security.impl;

import java.io.IOException;

import junit.framework.Assert;

import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;

public abstract class AbstractRoleServiceTest extends AbstractSecurityServiceTest {
    
    protected GeoServerRoleService service; 
    protected GeoServerRoleStore store; 


    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        service =   createRoleService("test");
        store  = createStore(service);
            
    }

            
    public void testIsModified() {
        try {
            
            assertFalse(store.isModified());
            
            insertValues(store);
            assertTrue(store.isModified());
            
            store.load();
            assertFalse(store.isModified());
            
            insertValues(store);
            store.store();
            assertFalse(store.isModified());
            
            GeoServerRole role = 
                store.createRoleObject("ROLE_DUMMY");
            GeoServerRole role_parent = 
                store.createRoleObject("ROLE_PARENT");        

            
            assertFalse(store.isModified());
            
            // add,remove,update
            store.addRole(role);
            store.addRole(role_parent);
            assertTrue(store.isModified());
            store.store();
            
            assertFalse(store.isModified());
            store.updateRole(role);
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.removeRole(role);
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.associateRoleToGroup(role, "agroup");
            assertTrue(store.isModified());
            store.store();

            assertFalse(store.isModified());
            store.disAssociateRoleFromGroup(role, "agroup");
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.associateRoleToUser(role, "auser");
            assertTrue(store.isModified());
            store.store();
            
            assertFalse(store.isModified());
            store.disAssociateRoleFromUser(role, "auser");
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.setParentRole(role,role_parent);
            assertTrue(store.isModified());
            store.store();
            
            assertFalse(store.isModified());
            store.setParentRole(role,null);
            assertTrue(store.isModified());
            store.store();


            assertFalse(store.isModified());
            store.clear();
            assertTrue(store.isModified());
            store.load();

            
        } catch ( IOException ex) {
            Assert.fail(ex.getMessage());
        }        
    }            

    
    public void testInsert() {
        try {
            

            // all is empty
            checkEmpty(service);
            checkEmpty(store);
        
            // transaction has values ?
            insertValues(store);
            if (!isJDBCTest())
                checkEmpty(service);
            checkValuesInserted(store);
            
            // rollback
            store.load();
            checkEmpty(store);
            checkEmpty(service);

            // commit
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            
        } catch ( IOException ex) {
            Assert.fail(ex.getMessage());
        }        
    }

    public void testModify() {
        try {
            
            checkEmpty(service);
            checkEmpty(store);
        
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            modifyValues(store);
            if (!isJDBCTest())
                checkValuesInserted(service);
            checkValuesModified(store);
            
            store.load();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            modifyValues(store);
            store.store();
            checkValuesModified(store);
            checkValuesModified(service);
            
                        
        } catch ( IOException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }        
    }

    public void testRemove() {
        try {
            
            // all is empty
            checkEmpty(service);
            checkEmpty(store);
        
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            removeValues(store);
            if (!isJDBCTest())
                checkValuesInserted(service);
            checkValuesRemoved(store);
            
            store.load();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            removeValues(store);
            store.store();
            checkValuesRemoved(store);
            checkValuesRemoved(service);
                        
        } catch ( IOException ex) {
            Assert.fail(ex.getMessage());
        }        
    }

}
