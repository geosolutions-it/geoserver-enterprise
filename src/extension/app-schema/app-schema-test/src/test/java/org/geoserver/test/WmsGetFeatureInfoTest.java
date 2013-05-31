/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import junit.framework.Test;

import org.geoserver.test.NamespaceTestData;
import org.w3c.dom.Document;

public class WmsGetFeatureInfoTest extends AbstractAppSchemaWfsTestSupport {

    public WmsGetFeatureInfoTest() throws Exception {
        super();
    }

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new WmsGetFeatureInfoTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected NamespaceTestData buildTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("positionalaccuracy21", "styles/positionalaccuracy21.sld");
        return mockData;
    }

   
    public void testGetCapabilities()
    {
        Document doc = getAsDOM("wms?request=GetCapabilities");
        LOGGER.info("WMS =GetCapabilities response:\n" + prettyString(doc));
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        assertXpathCount(1,"//wms:Layer/wms:Name[.='gsml:MappedFeature']", doc);
        assertXpathCount(1,"//wms:GetFeatureInfo/wms:Format[.='application/vnd.ogc.gml/3.1.1']", doc);
    }    
   
    public void testGetFeatureInfoText() throws Exception
    {
        String str = getAsString("wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100");
        LOGGER.info ( "WMS =GetFeatureInfo Text response:\n" + str);
        assertTrue(str.contains("FeatureImpl:MappedFeature<MappedFeatureType id=mf2>"));
    }
    
    public void testGetFeatureInfoGML() throws Exception
    {
    	String request = "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info ( "WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo("gu.25678", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@gml:id", doc);
        validateGet(request);
    }
    
    public void testGetFeatureInfoGML21() throws Exception
    {
    	String request = "wms?request=GetFeatureInfo&styles=positionalaccuracy21&SRS=EPSG:4326&BBOX=-1.3,53,0,53.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info ( "WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        validateGet(request);
    }
    
    public void testGetFeatureInfoHTML() throws Exception
    {
        Document doc = getAsDOM("wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=text/html");
        LOGGER.info ( "WMS =GetFeatureInfo HTML response:\n" + prettyString(doc));
        assertXpathCount(1, "/html/body/table/tr/td[.='mf2']", doc);
        assertXpathCount(1, "/html/body/table/tr/td/table[caption/.='CGI_TermValuePropertyType']/tr/td/table[caption/.='CGI_TermValueType']", doc);
        assertXpathCount(1, "/html/body/table/tr/td/table[caption/.='GeologicFeaturePropertyType']/tr/td/table[caption/.='GeologicUnitType']/tr/th[.='gml:description']", doc);
    }
    
    

}
