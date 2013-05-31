package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.*;
import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.wcs.test.WCSTestSupport;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class GetCapabilitiesTest extends WCSTestSupport {


    // @Override
    // protected String getDefaultLogConfiguration() {
    // return "/GEOTOOLS_DEVELOPER_LOGGING.properties";
    // }

    public void testGetBasic() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&version=1.0.0");
        // print(dom);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
    }
    
    public void testSkipMisconfigured() throws Exception {
        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        // manually misconfigure one layer
        CoverageStoreInfo cvInfo = getCatalog().getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart());
        cvInfo.setURL("file:///I/AM/NOT/THERE");
        getCatalog().save(cvInfo);
        
        // check we got everything but that specific layer, and that the output is still schema compliant
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&version=1.0.0");
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        
        int count = getCatalog().getCoverages().size();
        assertEquals(count - 1, dom.getElementsByTagName("wcs:CoverageOfferingBrief").getLength());
    }

    public void testNoServiceContactInfo() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS");
        // print(dom);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
    }

    public void testPostBasic() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:wcs=\"http://www.opengis.net/wcs\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        // print(dom);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
    }

    public void testUpdateSequenceInferiorGet() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&updateSequence=-1");
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:WCS_Capabilities", root.getNodeName());
        assertTrue(root.getChildNodes().getLength() > 0);
    }

    public void testUpdateSequenceInferiorPost() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                + " xmlns:wcs=\"http://www.opengis.net/wcs\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " updateSequence=\"-1\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
        final Node root = dom.getFirstChild();
        assertEquals("wcs:WCS_Capabilities", root.getNodeName());
        assertTrue(root.getChildNodes().getLength() > 0);
    }

    public void testUpdateSequenceEqualsGet() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0&updateSequence=0");
        // print(dom);
        final Node root = dom.getFirstChild();
        assertEquals("ServiceExceptionReport", root.getNodeName());
        assertEquals("CurrentUpdateSequence", root.getFirstChild().getNextSibling().getAttributes()
                .getNamedItem("code").getNodeValue());
    }

    public void testUpdateSequenceEqualsPost() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                + " xmlns:wcs=\"http://www.opengis.net/wcs\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " updateSequence=\"0\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        // print(dom);
        final Node root = dom.getFirstChild();
        assertEquals("ServiceExceptionReport", root.getNodeName());
        assertEquals("CurrentUpdateSequence", root.getFirstChild().getNextSibling().getAttributes()
                .getNamedItem("code").getNodeValue());
    }

    public void testUpdateSequenceSuperiorGet() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0&updateSequence=1");
        // print(dom);
        checkOws11Exception(dom);
    }

    public void testUpdateSequenceSuperiorPost() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wcs:GetCapabilities service=\"WCS\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                + " xmlns:wcs=\"http://www.opengis.net/wcs\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " updateSequence=\"1\" version=\"1.0.0\"/>";
        Document dom = postAsDOM(BASEPATH, request);
        checkOws11Exception(dom);
    }

    public void testSectionsBogus() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0&section=Bogus");
        checkOws11Exception(dom);
        assertXpathEvaluatesTo(WcsExceptionCode.InvalidParameterValue.toString(),
                "/ServiceExceptionReport/ServiceException/@code", dom);
    }

    public void testSectionsAll() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0&section=/");
        
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
        assertXpathEvaluatesTo("1", "count(//wcs:Service)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:Capability)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:ContentMetadata)", dom);
    }

    public void testOneSection() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0&section=/WCS_Capabilities/Service");
        assertXpathEvaluatesTo("1", "count(//wcs:Service)", dom);
        assertXpathEvaluatesTo("0", "count(//wcs:Capability)", dom);
        assertXpathEvaluatesTo("0", "count(//wcs:ContentMetadata)", dom);
    }
    
    public void testMetadataLinks() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/tasmania/dem.xml");
        ml.setMetadataType("FGDC");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);
        
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0");
        print(dom);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
        String xpathBase = "//wcs:CoverageOfferingBrief[wcs:name = '" + getLayerId(TASMANIA_DEM) + "']/wcs:metadataLink";
        assertXpathEvaluatesTo("http://www.geoserver.org", xpathBase + "/@about", dom);
        assertXpathEvaluatesTo("FGDC", xpathBase + "/@metadataType", dom);
        assertXpathEvaluatesTo("simple", xpathBase + "/@xlink:type", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org/tasmania/dem.xml", xpathBase + "/@xlink:href", dom);
    }
    
    public void testWorkspaceQualified() throws Exception {
        int expected = getCatalog().getCoverageStores().size();
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0");
        assertEquals( expected, xpath.getMatchingNodes("//wcs:CoverageOfferingBrief", dom).getLength());
        
        expected = getCatalog().getCoverageStoresByWorkspace(MockData.CDF_PREFIX).size();
        dom = getAsDOM("cdf/wcs?request=GetCapabilities&service=WCS&version=1.0.0");
        assertEquals( expected, xpath.getMatchingNodes("//wcs:CoverageOfferingBrief", dom).getLength());
    }
    
    public void testLayerQualified() throws Exception {
        int expected = getCatalog().getCoverageStores().size();
        Document dom = getAsDOM(BASEPATH
                + "?request=GetCapabilities&service=WCS&version=1.0.0");
        assertEquals( expected, xpath.getMatchingNodes("//wcs:CoverageOfferingBrief", dom).getLength());
        
        dom = getAsDOM("wcs/World/wcs?request=GetCapabilities&service=WCS&version=1.0.0");
        assertEquals( 1, xpath.getMatchingNodes("//wcs:CoverageOfferingBrief", dom).getLength());
    }
    
    public void testTimeCoverage() throws Exception {
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null);
        
        Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities&service=WCS&version=1.0.0");
        // print(dom);
        checkValidationErrors(dom, WCS10_GETCAPABILITIES_SCHEMA);
        
        // check the envelopes
        String base = "//wcs:CoverageOfferingBrief[wcs:name='wcs:watertemp']//wcs:lonLatEnvelope";
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", base + "/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", base + "/gml:timePosition[2]", dom);
    }
}
