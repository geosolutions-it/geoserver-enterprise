/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.security.iride;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import junit.framework.TestCase;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class IrideRoleServiceTest extends TestCase {
    private static final String BASE_SAMPLE_USER = "AAAAAA00A11D000L/DEMO 23/CSI PIEMONTE/CSI_NUOVACA/20131112095654/8/52paOWJH3ukdZkuV0A1ffg==";
    private static final String MAJOR_SAMPLE_USER = "AAAAAA00A11F000N/DEMO 25/CSI PIEMONTE/CSI_NUOVACA/20131112095654/8/52paOWJH3ukdZkuV0A1ffg==";
    private static final String SUPER_SAMPLE_USER = "AAAAAA00A11E000M/DEMO 24/CSI PIEMONTE/CSI_NUOVACA/20131112095654/8/52paOWJH3ukdZkuV0A1ffg==";
    File tempFolder;
    GeoServerSecurityManager securityManager;
    IrideSecurityProvider securityProvider;
    IrideSecurityServiceConfig config;
    
    @Override
    public void setUp() throws Exception {
    
        tempFolder = File.createTempFile("ldap", "test");
        tempFolder.delete();
        tempFolder.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(
                tempFolder);
        securityManager = new GeoServerSecurityManager(
                new GeoServerDataDirectory(resourceLoader));
        securityProvider = new IrideSecurityProvider(securityManager);
        config = new IrideSecurityServiceConfig();
        config.setServerURL("http://localhost:8085/iride2simApplIridepepWsfad/services/iride2simIridepep");
        config.setApplicationName("SIIG");
        config.setAdminRole("SUPERUSER_SIIG");
    }
    
    @Override
    public void tearDown() throws Exception {
        tempFolder.delete();
    }

    public void testGetRolesForBaseUser() throws IOException {
        GeoServerRoleService roleService = securityProvider.createRoleService(config);
        SortedSet<GeoServerRole> roles = roleService.getRolesForUser(BASE_SAMPLE_USER);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("BASEUSER_SIIG", roles.iterator().next().toString());
    }
    
    public void testGetRolesForMajorUser() throws IOException {
        GeoServerRoleService roleService = securityProvider.createRoleService(config);
        SortedSet<GeoServerRole> roles = roleService.getRolesForUser(MAJOR_SAMPLE_USER);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("MAJORUSER_SIIG", roles.iterator().next().toString());
    }
    
    public void testGetRolesForSuperUser() throws IOException {
        GeoServerRoleService roleService = securityProvider.createRoleService(config);
        SortedSet<GeoServerRole> roles = roleService.getRolesForUser(SUPER_SAMPLE_USER);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("SUPERUSER_SIIG", roles.iterator().next().toString());
        //assertEquals(GeoServerRole.ADMIN_ROLE, roles.iterator().next());
        
        RoleCalculator roleCalc = new RoleCalculator(roleService);
        roles = roleCalc.calculateRoles(SUPER_SAMPLE_USER);
        assertNotNull(roles);
        assertEquals(3, roles.size());
        boolean foundAdmin = false;
        for(GeoServerRole role : roles) {
            if(role.equals(GeoServerRole.ADMIN_ROLE)) {
                foundAdmin = true;
            }
        }
        assertTrue(foundAdmin);
    }
}
