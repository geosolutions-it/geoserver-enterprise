/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * 
 * 
 * @author Niels Charlier (Curtin University of Technology)
 * 
 */
public class IdNotEncodedTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new IdNotEncodedTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new IdNotEncodedMockData();
    }

    /**
     * Test whether GetFeature
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedInterval");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("ubspatial.hydrostratigraphicunit.123",
                "//gsml:MappedInterval[@gml:id='123']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id", doc);
        assertXpathEvaluatesTo("ubspatial.hydrostratigraphicunit.456",
                "//gsml:MappedInterval[@gml:id='456']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id", doc);
        assertXpathEvaluatesTo("ubspatial.hydrostratigraphicunit.789",
                "//gsml:MappedInterval[@gml:id='789']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id", doc);
        assertXpathEvaluatesTo("ubspatial.hydrostratigraphicunit.012",
                "//gsml:MappedInterval[@gml:id='012']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id", doc);
       
    }

}
