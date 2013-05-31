/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Test whether nested Id's can be used in a filter.
 * 
 * @author Niels Charlier, Curtin University Of Technology *
 */

public class NestedIdSupportTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new NestedIdSupportTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new NestedIdSupportTestData();
    }

    /**
     * Test Nested Id with Feature Chaining
     */
    public void testNestedIdFeatureChaining() {
        String xml = "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" " //
                + ">" //
                + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                + "<ogc:Filter>"
                + "     <ogc:PropertyIsEqualTo>"
                + "        <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/gsml:ControlledConcept/@gml:id</ogc:PropertyName>"
                + "        <ogc:Literal>cc.1</ogc:Literal>"
                + "     </ogc:PropertyIsEqualTo>"
                + " </ogc:Filter>" + "</wfs:Query>" + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("MappedFeature: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4",
                "wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);

    }

    /**
     * Test Nested Id with InlineMapping
     * */
    public void testNestedIdInlineMapping() {
        String xml = "<wfs:GetFeature " //
                + "service=\"WFS\" " //
                + "version=\"1.1.0\" " //
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "xmlns:gsml=\""
                + AbstractAppSchemaMockData.GSML_URI
                + "\" " //
                + ">" //
                + "<wfs:Query typeName=\"gsml:Borehole\">"
                + "<ogc:Filter>"
                + "     <ogc:PropertyIsEqualTo>"
                + "        <ogc:PropertyName>gsml:indexData/gsml:BoreholeDetails/@gml:id</ogc:PropertyName>"
                + "        <ogc:Literal>bh.details.11.sp</ogc:Literal>"
                + "     </ogc:PropertyIsEqualTo>" + " </ogc:Filter>"
                + "</wfs:Query>"
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("Borehole: WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:Borehole", doc);
        assertXpathEvaluatesTo("11",
                "wfs:FeatureCollection/gml:featureMember/gsml:Borehole/@gml:id", doc);
    }

}
