package org.geoserver.test.onlineTest;

import junit.framework.Test;

import org.geotools.data.complex.AppSchemaDataAccessRegistry;

public class DataReferenceWfsPostgisWithJoiningTest extends DataReferenceWfsPostgisTest {

    public DataReferenceWfsPostgisWithJoiningTest() throws Exception {
        super();
    }

    /**
     * Read-only test so can use one-time setup.
     *
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new DataReferenceWfsPostgisWithJoiningTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "true");
        super.oneTimeSetUp();
    }
    
    @Override
    public void testFilteringSplit() throws Exception {
        //this is a non joining test
    }

}
