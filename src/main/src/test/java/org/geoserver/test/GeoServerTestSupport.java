/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;



/**
 * Base test class for GeoServer unit tests.
 * <p>
 * Deriving from this test class provides the test case with preconfigured
 * geoserver and catalog objects.
 * </p>
 * <p>
 * This test case provides a spring application context which loads the
 * application contexts from all modules on the classpath.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class GeoServerTestSupport extends GeoServerAbstractTestSupport {
    
   
    @Override
    public MockData buildTestData() throws Exception {
        // create the data directory
        MockData dataDirectory = new MockData();
        populateDataDirectory(dataDirectory);
        return dataDirectory;
    }
    
    public MockData getTestData() {
        return (MockData) super.getTestData();
    }
    
    /** 
     * Adds the desired type and coverages to the data directory. This method adds all well known
     * data types, subclasses may add their extra ones or decide to avoid the standar ones and 
     * build a custom list calling {@link MockData#addPropertiesType(QName, java.net.URL, java.net.URL)}
     * and {@link MockData#addCoverage(QName, InputStream, String)}
     * @throws IOException
     */
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        //set up the data directory
        dataDirectory.addWellKnownTypes(MockData.TYPENAMES);
    }
    
    /**
     * Sets up a template in a feature type directory.
     * 
     * @param featureTypeName The name of the feature type.
     * @param template The name of the template.
     * @param body The content of the template.
     * 
     * @throws IOException
     */
    protected void setupTemplate(QName featureTypeName,String template,String body)
        throws IOException {
        
        getTestData().copyToFeatureTypeDirectory( new ByteArrayInputStream(body.getBytes()), featureTypeName, template );
    }

    protected void addUser(String username, String password, List<String> groups, List<String> roles) throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerUserGroupService ugService = secMgr.loadUserGroupService("default");

        GeoServerUserGroupStore ugStore = ugService.createStore();
        GeoServerUser user = ugStore.createUserObject(username, password, true);
        ugStore.addUser(user);

        if (groups != null && !groups.isEmpty()) {
            for (String groupName : groups) {
                GeoServerUserGroup group = ugStore.getGroupByGroupname(groupName);
                if (group == null) {
                    group = ugStore.createGroupObject(groupName, true);
                    ugStore.addGroup(group);
                }
    
                ugStore.associateUserToGroup(user, group);
            }
        }
        ugStore.store();

        if (roles != null && !roles.isEmpty()) {
            GeoServerRoleService roleService = secMgr.getActiveRoleService();
            GeoServerRoleStore roleStore = roleService.createStore();
            for (String roleName : roles) {
                GeoServerRole role = roleStore.getRoleByName(roleName);
                if (role == null) {
                    role = roleStore.createRoleObject(roleName);
                    roleStore.addRole(role);
                }

                roleStore.associateRoleToUser(role, username);
            }

            roleStore.store();
        }
    }
    
}
