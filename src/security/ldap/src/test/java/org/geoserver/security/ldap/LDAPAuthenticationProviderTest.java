/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;


import java.io.File;
import java.util.Collections;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import junit.framework.TestCase;

import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.partition.impl.btree.MutableBTreePartitionConfiguration;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * 
 */
public class LDAPAuthenticationProviderTest extends TestCase {
    private LDAPSecurityProvider securityProvider;
    private GeoServerSecurityManager securityManager;
    private LDAPSecurityServiceConfig config;
    private LDAPAuthenticationProvider authProvider;
    private Authentication authentication;
    private Authentication authenticationOther;
    private File tempFolder;

    private static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    private static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @Override
    public void setUp() throws Exception {

        tempFolder = File.createTempFile("ldap", "test");
        tempFolder.delete();
        tempFolder.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(
                tempFolder);
        securityManager = new GeoServerSecurityManager(
                new GeoServerDataDirectory(resourceLoader));
        securityProvider = new LDAPSecurityProvider(securityManager);
        config = new LDAPSecurityServiceConfig();
        config.setServerURL(ldapServerUrl + "/" + basePath);
        config.setGroupSearchBase("ou=Groups");
        config.setGroupSearchFilter("member=cn={1}");
        config.setUseTLS(false);

        authentication = new UsernamePasswordAuthenticationToken("admin",
                "admin");
        authenticationOther = new UsernamePasswordAuthenticationToken("other",
                "other");
    }

    @Override
    public void tearDown() throws Exception {
        tempFolder.delete();

        LdapTestUtils
                .destroyApacheDirectoryServer(LdapTestUtils.DEFAULT_PRINCIPAL,
                        LdapTestUtils.DEFAULT_PASSWORD);
    }

    /**
     * Test that bindBeforeGroupSearch correctly enables roles fetching on a
     * server without anonymous access enabled.
     * 
     * @throws Exception
     */
    public void testBindBeforeGroupSearch() throws Exception {
        // no anonymous access
        if(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authentication);
            assertNotNull(result);
            assertEquals("admin", result.getName());
            assertEquals(2, result.getAuthorities().size());
        }

    }

    /**
     * Test that without bindBeforeGroupSearch we get an exception during roles
     * fetching on a server without anonymous access enabled.
     * 
     * @throws Exception
     */    
    public void testBindBeforeGroupSearchRequiredIfAnonymousDisabled()
            throws Exception {
        // no anonymous access
        if(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath)) {
            config.setUserDnPattern("uid={0},ou=People");
            // we don't bind
            config.setBindBeforeGroupSearch(false);
            createAuthenticationProvider();
            boolean error = false;
            try {
                authProvider.authenticate(authentication);
            } catch (Exception e) {
                error = true;
            }
            assertTrue(error);
        }

    }

    /**
     * Test that authentication can be done using the couple userFilter and
     * userFormat instead of userDnPattern.
     * 
     * @throws Exception
     */
    public void testUserFilterAndFormat() throws Exception {
        if(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath)) {    
            // filter to extract user data
            config.setUserFilter("(telephonenumber=1)");
            // username to bind to
            config.setUserFormat("uid={0},ou=People,dc=example,dc=com");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authentication);
            assertEquals(2, result.getAuthorities().size());
        }
    }

    /**
     * Test that if and adminGroup is defined, the roles contain
     * ROLE_ADMINISTRATOR
     * 
     * @throws Exception
     */
    public void testAdminGroup() throws Exception {
        if(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setAdminGroup("other");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRATOR")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }
    }

    /**
     * Test that if and groupAdminGroup is defined, the roles contain
     * ROLE_GROUP_ADMIN
     * 
     * @throws Exception
     */
    public void testGroupAdminGroup() throws Exception {
        if(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setGroupAdminGroup("other");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_GROUP_ADMIN")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }
    }
    

    private void createAuthenticationProvider() {
        authProvider = (LDAPAuthenticationProvider) securityProvider
                .createAuthenticationProvider(config);

    }
}
