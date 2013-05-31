/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.auth;

import java.security.Principal;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.filter.GeoServerDigestAuthenticationFilter;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geotools.data.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class AuthenticationCacheFilterTest extends AbstractAuthenticationProviderTest {
    
    public final static String testFilterName = "basicAuthTestFilter";
    public final static String testFilterName2 = "digestAuthTestFilter";
    public final static String testFilterName3 = "j2eeAuthTestFilter";
    public final static String testFilterName4 = "requestHeaderTestFilter";
    public final static String testFilterName5 = "basicAuthTestFilterWithRememberMe";
    public final static String testFilterName8 = "x509TestFilter";


    Authentication getAuth(String filterName, String user, Integer idleTime, Integer liveTime) {
        
        Map<String,byte[]> map= getCache().cache.get(filterName);
        if (map==null)
            return null;
        Authentication result=null;
        String cacheKey=null;
        for ( Entry<String,byte[]> entry : map.entrySet()) {            
            Authentication auth = getCache().deserializeAuthentication(entry.getValue()); 
            Object o = auth.getPrincipal();
            
            if (o instanceof UserDetails) {
                if (user.equals(((UserDetails)o).getUsername())) {
                    result= auth;
                    cacheKey=entry.getKey();
                    break;
                }
                                        
            }
            if (o instanceof Principal) {
                if (user.equals(((Principal)o).getName())) {
                     result= auth;
                     cacheKey=entry.getKey();
                     break;
                }
            }
            if (o instanceof String) {
                if (user.equals(((String)o))) {
                        result= auth;
                        cacheKey=entry.getKey();
                        break;
                }
            }                
        }
        
        if (result != null) {
            Integer[] seconds = getCache().getExpireTimes(filterName, cacheKey);
            if (idleTime==null)
                assertEquals(TestingAuthenticationCache.DEFAULT_IDLE_SECS, seconds[0]);
            else
                assertEquals(idleTime, seconds[0]);
            
            if (liveTime==null)
                assertEquals(TestingAuthenticationCache.DEFAULT_LIVE_SECS, seconds[1]);
            else
                assertEquals(liveTime, seconds[1]);
        }

        return result;
    }
    public void testBasicAuth() throws Exception{
        
                
        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        config.setUseRememberMe(false);
        config.setName(testFilterName);
        
        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
            testFilterName,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();        
        
        
        getProxy().doFilter(request, response, chain);
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Basic") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        
        // check success
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((testUserName+":"+testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        Authentication auth = getAuth(testFilterName, testUserName,null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        // check wrong password
//        request= createRequest("/foo/bar");
//        response= new MockHttpServletResponse();
//        chain = new MockFilterChain();
//
//        request.addHeader("Authorization",  "Basic " + 
//                new String(Base64.encodeBytes((testUserName+":wrongpass").getBytes())));
//        getProxy().doFilter(request, response, chain);
//        tmp = response.getHeader("WWW-Authenticate");
//        assertNotNull(tmp);
//        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
//        assert(tmp.indexOf("Basic") !=-1 );
//        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
//        assertNull(SecurityContextHolder.getContext().getAuthentication());
//        auth = getAuth(testFilterName, testUserName,null,null);
//        assertNull(auth);

        
        // check unknown user
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();

        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes(("unknwon:"+testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Basic") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        auth = getAuth("unknow", testPassword,null,null);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((GeoServerUser.ROOT_USERNAME+":"+getMasterPassword()).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(GeoServerUser.ROOT_USERNAME, "geoserver",null,null);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        

        
        // check disabled user        
        updateUser("ug1", testUserName, false);
        
        // since the cache is working, disabling has no effect
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((testUserName+":"+testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName, testUserName,null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // clear cache, user should be disabled
        getCache().removeAll();

        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((testUserName+":"+testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Basic") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        updateUser("ug1", testUserName, true);
        
    }
    
    public void testJ2eeProxy() throws Exception{

        J2eeAuthenticationFilterConfig config = new J2eeAuthenticationFilterConfig();        
        config.setClassName(GeoServerJ2eeAuthenticationFilter.class.getName());        
        config.setName(testFilterName3);
        config.setRoleServiceName("rs1");        
        getSecurityManager().saveFilter(config);
        
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
            testFilterName3,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();                
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN,response.getErrorCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());


        // test preauthenticated with dedicated role service        
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                
        request.setUserPrincipal(new Principal() {            
            @Override
            public String getName() {
                return testUserName;
            }
        });
        request.setUserInRole(derivedRole,true);
        request.setUserInRole(rootRole,false);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        Authentication auth = getAuth(testFilterName3, testUserName,null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        // test root                
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                
        request.setUserPrincipal(new Principal() {            
            @Override
            public String getName() {
                return GeoServerUser.ROOT_USERNAME;
            }
        });
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName3, GeoServerUser.ROOT_USERNAME,null,null);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        //checkForAuthenticatedRole(auth);

        config.setRoleServiceName(null);
        getSecurityManager().saveFilter(config);
        
        // test preauthenticated with active role service                
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                
        request.setUserPrincipal(new Principal() {            
            @Override
            public String getName() {
                return testUserName;
            }
        });
        request.setUserInRole(derivedRole,true);
        request.setUserInRole(rootRole,false);
        getProxy().doFilter(request, response, chain);
        
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName3, testUserName,null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        // Test anonymous
        insertAnonymousFilter(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

                
    }
    
    public void testRequestHeaderProxy() throws Exception{

        RequestHeaderAuthenticationFilterConfig config = 
                new RequestHeaderAuthenticationFilterConfig();        
        config.setClassName(GeoServerRequestHeaderAuthenticationFilter.class.getName());        
        config.setName(testFilterName4);
        config.setRoleServiceName("rs1");
        config.setPrincipalHeaderAttribute("principal");
        config.setRoleSource(RoleSource.RoleService);
        config.setUserGroupServiceName("ug1");
        config.setPrincipalHeaderAttribute("principal");
        config.setRolesHeaderAttribute("roles");;
        getSecurityManager().saveFilter(config);
        
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
            testFilterName4,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();                
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN,response.getErrorCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        
        for (RoleSource rs : RoleSource.values()) {            
            getCache().removeAll();
            
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);
            request= createRequest("/foo/bar");
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();            
            request.setHeader("principal", testUserName);
            if (rs==RoleSource.Header) {
                request.setHeader("roles", derivedRole+";"+rootRole);
            }
            getProxy().doFilter(request, response, chain);            
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            Authentication auth = getAuth(testFilterName4, testUserName,null,null);
            if (rs==RoleSource.Header) {
                continue; // no cache
            }
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals(testUserName, auth.getPrincipal());
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));        
        }

        // unknown user
        for (RoleSource rs : RoleSource.values()) {
            getCache().removeAll();            
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);

            config.setRoleSource(rs);
            request= createRequest("/foo/bar");
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();            
            request.setHeader("principal", "unknown");
            getProxy().doFilter(request, response, chain);            
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            if (rs==RoleSource.Header) {
                continue; // no cache
            }
            Authentication auth = getAuth(testFilterName4, "unknown",null,null);
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals("unknown", auth.getPrincipal());
        }

        // test disabled user, should not work since cache is active 
        
        config.setRoleSource(RoleSource.UserGroupService);
        // saving a filter empties the cache
        getSecurityManager().saveFilter(config);
        updateUser("ug1", testUserName, false);
                
        request= createRequest("/foo/bar");
        request.setHeader("principal", testUserName);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();            
        getProxy().doFilter(request, response, chain);                                    
        assertEquals(HttpServletResponse.SC_FORBIDDEN,response.getErrorCode());
        Authentication auth = getAuth(testFilterName4, testUserName,null,null);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());        
        // Test anonymous
        insertAnonymousFilter(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

                
    }        


    public void testDigestAuth() throws Exception{

        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName(testFilterName2);
        config.setUserGroupServiceName("ug1");
        
        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern,
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
                testFilterName2,
                GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
                GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
            
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();                
            
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED,response.getErrorCode());
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Digest") !=-1 );
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        
        // test successful login
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        String headerValue=clientDigestString(tmp, testUserName, testPassword, request.getMethod());
        request.addHeader("Authorization",  headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        Authentication auth = getAuth(testFilterName2, testUserName,300,300);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        
        // check wrong password
//        request= createRequest("/foo/bar");
//        response= new MockHttpServletResponse();
//        chain = new MockFilterChain();
//        
//        headerValue=clientDigestString(tmp, testUserName, "wrongpass", request.getMethod());
//        request.addHeader("Authorization",  headerValue);        
//        getProxy().doFilter(request, response, chain);
//        tmp = response.getHeader("WWW-Authenticate");
//        assertNotNull(tmp);
//        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
//        assert(tmp.indexOf("Digest") !=-1 );
//        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
//        auth = getAuth(testFilterName2, testUserName,300,300);
//        assertNull(auth);
//        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        // check unknown user
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();

        headerValue=clientDigestString(tmp, "unknown", testPassword, request.getMethod());
        request.addHeader("Authorization",  headerValue);        
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Digest") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        auth = getAuth(testFilterName2, "unknown",300,300);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // check root user
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        
        headerValue=clientDigestString(tmp, GeoServerUser.ROOT_USERNAME, getMasterPassword(), request.getMethod());
        request.addHeader("Authorization",  headerValue);        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName2, GeoServerUser.ROOT_USERNAME,300,300);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        // check root user with wrong password
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
        
        headerValue=clientDigestString(tmp, GeoServerUser.ROOT_USERNAME, "geoserver1", request.getMethod());
        request.addHeader("Authorization",  headerValue);        
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Digest") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        auth = getAuth(testFilterName2, GeoServerUser.ROOT_USERNAME,300,300);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());


        
        // check disabled user, should not work becaus of cache
        updateUser("ug1", testUserName, false);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        headerValue=clientDigestString(tmp, testUserName, testPassword, request.getMethod());
        request.addHeader("Authorization",  headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName2, testUserName,300,300);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // clear cache, now disabling should work
        getCache().removeAll();
        
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        headerValue=clientDigestString(tmp, "unknown", testPassword, request.getMethod());
        request.addHeader("Authorization",  headerValue);        
        getProxy().doFilter(request, response, chain);
        tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Digest") !=-1 );
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        auth = getAuth(testFilterName2, testUserName,300,300);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());        
        updateUser("ug1", testUserName, true);        


        // Test anonymous
        insertAnonymousFilter(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();
    }

    public void testBasicAuthWithRememberMe() throws Exception{
    
        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        config.setUseRememberMe(true);
        config.setName(testFilterName5);
        
        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
            testFilterName5,
            GeoServerSecurityFilterChain.REMEMBER_ME_FILTER,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);
    
    
        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();        
                
        getProxy().doFilter(request, response, chain);
        assertEquals(0, response.getCookies().size());
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
    
        
        // check success
        request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
    
                
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes(("abc@xyz.com:abc").getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        Authentication auth = getAuth(testFilterName5, "abc@xyz.com", null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(1,response.getCookies().size());
        Cookie cookie = (Cookie) response.getCookies().get(0);

        request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        request.addCookie(cookie);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName5, "abc@xyz.com", null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals("abc@xyz.com", ((UserDetails) auth.getPrincipal()).getUsername());
//        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
//        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

        // send cookie + auth header
        request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        request.addCookie(cookie);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes(("abc@xyz.com:abc").getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName5, "abc@xyz.com", null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals("abc@xyz.com", ((UserDetails) auth.getPrincipal()).getUsername());

        // check no remember me for root user
        request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        
    
                
        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((GeoServerUser.ROOT_USERNAME+":"+getMasterPassword()).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName5, GeoServerUser.ROOT_USERNAME, null,null);        
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        //checkForAuthenticatedRole(auth);
        // no cookie for root user
        assertEquals(0,response.getCookies().size());
        
        // check disabled user
        updateUser("ug1", "abc@xyz.com", false);
        
        request= createRequest("/foo/bar");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        request.addCookie(cookie);
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getErrorCode());
        // check for cancel cookie
        assertEquals(1,response.getCookies().size());
        Cookie cancelCookie = (Cookie) response.getCookies().get(0);
        assertNull(cancelCookie.getValue());
        updateUser("ug1", "abc@xyz.com", true);

        
    }

    public void testX509Auth() throws Exception{

        X509CertificateAuthenticationFilterConfig config = 
                new X509CertificateAuthenticationFilterConfig();        
        config.setClassName(GeoServerX509CertificateAuthenticationFilter.class.getName());        
        config.setName(testFilterName8);
        config.setRoleServiceName("rs1");
        config.setRoleSource(org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource.RoleService);
        config.setUserGroupServiceName("ug1");
        config.setRolesHeaderAttribute("roles");
        getSecurityManager().saveFilter(config);
        
        prepareFilterChain(pattern,
            GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,    
            testFilterName8,
            GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
        
        // Test entry point                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();                
        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_FORBIDDEN,response.getErrorCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        
        for (org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource rs : 
            org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource.values()) {
            getCache().removeAll();
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);
            request= createRequest("/foo/bar");
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();
            if (rs==RoleSource.Header) {
                request.setHeader("roles", derivedRole+";"+rootRole);
            }
            setCertifacteForUser(testUserName, request);                        
            getProxy().doFilter(request, response, chain);            
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            
            if (rs==RoleSource.Header) {
                continue; // no cache
            }
            Authentication auth = getAuth(testFilterName8, testUserName,null,null);
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals(testUserName, auth.getPrincipal());
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
            assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));        
        }

        // unknown user
        for (org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource rs : 
            org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource.values()) {
            getCache().removeAll();
            config.setRoleSource(rs);
            getSecurityManager().saveFilter(config);

            config.setRoleSource(rs);
            request= createRequest("/foo/bar");
            response= new MockHttpServletResponse();
            chain = new MockFilterChain();
            //TODO
            setCertifacteForUser("unknown", request);
            getProxy().doFilter(request, response, chain);            
            assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
            if (rs==RoleSource.Header) {
                continue; // no cache
            }
            Authentication auth = getAuth(testFilterName8, "unknown",null,null);
            assertNotNull(auth);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            checkForAuthenticatedRole(auth);
            assertEquals("unknown", auth.getPrincipal());
        }

        // test disabled user, should not work because of active cache
        updateUser("ug1", testUserName, false);
        config.setRoleSource(org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource.UserGroupService);
        // saving the filter clears the cache
        getSecurityManager().saveFilter(config);
                
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();
        setCertifacteForUser(testUserName, request);
        getProxy().doFilter(request, response, chain);            
        assertEquals(HttpServletResponse.SC_FORBIDDEN,response.getErrorCode());
        Authentication auth = getAuth(testFilterName8, testUserName,0,0);
        assertNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        // Test anonymous
        insertAnonymousFilter(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();                        
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        // Anonymous context is not stored in http session, no further testing
        removeAnonymousFilter();

                
    }      
    
    public void testCascadingFilters() throws Exception{

        BasicAuthenticationFilterConfig bconfig = new BasicAuthenticationFilterConfig();
        bconfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        bconfig.setUseRememberMe(false);
        bconfig.setName(testFilterName);
        getSecurityManager().saveFilter(bconfig);

        
        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName(testFilterName2);
        config.setUserGroupServiceName("ug1");
        
        getSecurityManager().saveFilter(config);
        prepareFilterChain(pattern,
                GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER,
                testFilterName,
                testFilterName2,
                GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
                GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);


        SecurityContextHolder.getContext().setAuthentication(null);
            
        // Test entry point, must be digest                
        MockHttpServletRequest request= createRequest("/foo/bar");
        MockHttpServletResponse response= new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();                
            
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED,response.getErrorCode());
        String tmp = response.getHeader("WWW-Authenticate");
        assertNotNull(tmp);
        assert(tmp.indexOf(GeoServerSecurityManager.REALM) !=-1 );
        assert(tmp.indexOf("Digest") !=-1 );
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        
        
        // test successful login for digest
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        String headerValue=clientDigestString(tmp, testUserName, testPassword, request.getMethod());
        request.addHeader("Authorization",  headerValue);
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        Authentication auth = getAuth(testFilterName2, testUserName, 300,300);        
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        
        // check success for basic authentication
        request= createRequest("/foo/bar");
        response= new MockHttpServletResponse();
        chain = new MockFilterChain();        

        request.addHeader("Authorization",  "Basic " + 
                new String(Base64.encodeBytes((testUserName+":"+testPassword).getBytes())));
        getProxy().doFilter(request, response, chain);
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());
        auth = getAuth(testFilterName, testUserName, null,null);
        assertNotNull(auth);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        checkForAuthenticatedRole(auth);
        assertEquals(testUserName, ((UserDetails) auth.getPrincipal()).getUsername());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(rootRole)));
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));

    }
    
}
