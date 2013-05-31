package org.geoserver.security.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.decorators.ReadOnlyDataStoreTest;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;

public class SecureCatalogImplTest extends AbstractAuthorizationTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        populateCatalog();
    }

    public void testWideOpen() throws Exception {
        ResourceAccessManager manager = buildManager("wideOpen.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // use no user at all
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }

    public void testLockedDown() throws Exception {
        ResourceAccessManager manager = buildManager("lockedDown.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertNull(sc.getCoverageByName("nurc:arcgrid"));
        assertNull(sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertNull(sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(0, sc.getFeatureTypes().size());
        assertEquals(0, sc.getCoverages().size());
        assertEquals(0, sc.getWorkspaces().size());
        assertNull(sc.getWorkspaceByName("topp"));
        assertNull(sc.getDataStoreByName("states"));
        assertNull(sc.getDataStoreByName("roads"));
        assertNull(sc.getCoverageStoreByName("arcGrid"));

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }
    
    public void testLockedChallenge() throws Exception {
        ResourceAccessManager manager = buildManager("lockedDownChallenge.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // check a direct access to the data does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states").getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid").getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");

        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class).getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class).getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        sc.getWorkspaceByName("topp");
        try {
            sc.getDataStoreByName("states").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid").getFormat();
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        
        // check we still get the lists out so that capabilities can be built
        assertEquals(featureTypes.size(), sc.getFeatureTypes().size());
        assertEquals(coverages.size(), sc.getCoverages().size());
        assertEquals(workspaces.size(), sc.getWorkspaces().size());
        

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }
    
    public void testLockedMixed() throws Exception {
        ResourceAccessManager manager = buildManager("lockedDownMixed.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // try with read only user and GetFeatures request
        SecurityContextHolder.getContext().setAuthentication(roUser);
        Request request = org.easymock.classextension.EasyMock.createNiceMock(Request.class);
        org.easymock.classextension.EasyMock.expect(request.getRequest()).andReturn("GetFeatures").anyTimes();
        org.easymock.classextension.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);

        // check a direct access does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getWorkspaceByName("topp");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("states");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        
        // try with a getCapabilities, make sure the lists are empty
        request = org.easymock.classextension.EasyMock.createNiceMock(Request.class);
        org.easymock.classextension.EasyMock.expect(request.getRequest()).andReturn("GetCapabilities").anyTimes();
        org.easymock.classextension.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);
        
        // check the lists used to build capabilities are empty
        assertEquals(0, sc.getFeatureTypes().size());
        assertEquals(0, sc.getCoverages().size());
        assertEquals(0, sc.getWorkspaces().size());
        
        

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }

    public void testPublicRead() throws Exception {
        ResourceAccessManager manager = buildManager("publicRead.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
        // .. the following should have been wrapped
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getFeatureTypeByName("topp:states") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getResourceByName("topp:states", FeatureTypeInfo.class) instanceof SecuredFeatureTypeInfo);
        assertEquals(featureTypes.size(), sc.getFeatureTypes().size());
        for (FeatureTypeInfo ft : sc.getFeatureTypes()) {
            assertTrue(ft instanceof SecuredFeatureTypeInfo);
        }
        assertNotNull(sc.getLayerByName("topp:states"));
        assertTrue(sc.getLayerByName("topp:states") instanceof SecuredLayerInfo);
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);

        // try with write enabled user (nothing has been wrapped)
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }

    public void testComplex() throws Exception {
        ResourceAccessManager manager = buildManager("complex.properties");
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager);

        // try with anonymous user
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        // ... roads follows generic ns rule, read only, nobody can write it
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        // ... states requires READER role
        assertNull(sc.getFeatureTypeByName("topp:states"));
        // ... but the datastore is visible since the namespace rules do apply instead
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        // ... landmarks requires WRITER role to be written
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        // ... bases requires one to be in the military
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // ok, let's try the same with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:states") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // now with the write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertSame(landmarks, sc.getFeatureTypeByName("topp:landmarks"));
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // finally let's try the military type
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        // ... bases requires one to be in the military
        assertSame(bases, sc.getFeatureTypeByName("topp:bases"));
    }
     
}
