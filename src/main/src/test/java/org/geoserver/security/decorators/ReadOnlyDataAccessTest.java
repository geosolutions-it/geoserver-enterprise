package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.geoserver.security.SecurityUtils;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.FeatureType;

public class ReadOnlyDataAccessTest extends SecureObjectsTest {

    private DataAccess da;

    private NameImpl name;

    protected void setUp() throws Exception {
        super.setUp();

        FeatureSource fs = createNiceMock(FeatureSource.class);
        replay(fs);
        FeatureType schema = createNiceMock(FeatureType.class);
        replay(schema);
        da = createNiceMock(DataAccess.class);
        name = new NameImpl("blah");
        expect(da.getFeatureSource(name)).andReturn(fs);
        replay(da);
    }

    public void testDontChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.hide(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isHide());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.readOnlyChallenge(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isReadOnlyChallenge());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e)==false)
                fail("Should have thrown a security exception...");            
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e)==false)
                fail("Should have thrown a security exception...");            

        }
    }

}
