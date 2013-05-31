package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;

public class GetPropertyValueTest extends WFS20TestSupport {

    public void testPOST() throws Exception {
        String xml = 
            "<wfs:GetPropertyValue service='WFS' version='2.0.0' "
                + "xmlns:sf='" + MockData.SF_URI + "'    "
                + "xmlns:fes='http://www.opengis.net/fes/2.0' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' valueReference='pointProperty'> "
                + "<wfs:Query typeNames='sf:PrimitiveGeoFeature'/> "
            + "</wfs:GetPropertyValue>";
        
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member/sf:pointProperty/gml:Point)", dom);
    }
    
    public void testGET() throws Exception {
        Document dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetPropertyValue" +
            "&typeNames=sf:PrimitiveGeoFeature&valueReference=pointProperty");
        
        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member)", dom);
        XMLAssert.assertXpathEvaluatesTo("3", "count(//wfs:member/sf:pointProperty/gml:Point)", dom);
    }

    public void testResolveException() throws Exception {
        String xml = 
            "<wfs:GetPropertyValue service='WFS' version='2.0.0' "
                + "xmlns:sf='" + MockData.SF_URI + "'    "
                + "xmlns:fes='http://www.opengis.net/fes/2.0' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " 
                + "valueReference='pointProperty' resolve='none'> "
                + "<wfs:Query typeNames='sf:PrimitiveGeoFeature'/> "
            + "</wfs:GetPropertyValue>";
            
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:ValueCollection", dom.getDocumentElement().getNodeName());
        
        xml = 
            "<wfs:GetPropertyValue service='WFS' version='2.0.0' "
                    + "xmlns:sf='" + MockData.SF_URI + "'    "
                    + "xmlns:fes='http://www.opengis.net/fes/2.0' "
                    + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " 
                    + "valueReference='pointProperty' resolve='local'> "
                    + "<wfs:Query typeNames='sf:PrimitiveGeoFeature'/> "
           + "</wfs:GetPropertyValue>";
        
        dom = postAsDOM("wfs", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("InvalidParameterValue", "//ows:Exception/@exceptionCode", dom);
        XMLAssert.assertXpathEvaluatesTo("resolve", "//ows:Exception/@locator", dom);
    }
}
