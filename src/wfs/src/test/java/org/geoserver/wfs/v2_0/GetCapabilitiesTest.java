package org.geoserver.wfs.v2_0;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletResponse;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetCapabilitiesTest extends WFS20TestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCapabilitiesTest());
    }
    
    public void testGet() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");
        
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength() > 0);
    }
    
    public void testPost() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }
    
    public void testNamespaceFilter() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities&namespace=sf");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());
        
        // try again with a missing one
        doc = getAsDOM("wfs?service=WFS&request=getCapabilities&namespace=NotThere");
        e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength());
    }

    public void testPostNoSchemaLocation() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" version='2.0.0' "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }
    
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");
        
         print(doc);

        // let's look for the outputFormat parameter values inside of the GetFeature operation metadata
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList formats = engine.getMatchingNodes(
                "//ows:Operation[@name=\"GetFeature\"]/ows:Parameter[@name=\"outputFormat\"]/ows:AllowedValues/ows:Value", doc);
        
        Set<String> s1 = new TreeSet<String>();
        for ( int i = 0; i < formats.getLength(); i++ ) {
            String format = formats.item(i).getFirstChild().getNodeValue();
            s1.add( format );
        }
        
        List<WFSGetFeatureOutputFormat> extensions = GeoServerExtensions.extensions( WFSGetFeatureOutputFormat.class );
        
        Set<String> s2 = new TreeSet<String>();
        for ( Iterator e = extensions.iterator(); e.hasNext(); ) {
            WFSGetFeatureOutputFormat extension = (WFSGetFeatureOutputFormat) e.next();
            s2.addAll( extension.getOutputFormats() );
        }
        
        assertEquals( s1, s2 );
    }

    public void testSupportedSpatialOperators() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        // let's look for the spatial capabilities, extract all the spatial operators
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList spatialOperators = engine
                .getMatchingNodes(
                        "//fes:Spatial_Capabilities/fes:SpatialOperators/fes:SpatialOperator/@name",
                        doc);

        Set<String> ops = new TreeSet<String>();
        for (int i = 0; i < spatialOperators.getLength(); i++) {
            String format = spatialOperators.item(i).getFirstChild()
                    .getNodeValue();
            ops.add(format);
        }

        List<String> expectedSpatialOperators = getSupportedSpatialOperatorsList(false);
        assertEquals(expectedSpatialOperators.size(), ops.size());
        assertTrue(ops.containsAll(expectedSpatialOperators));
    }

    public void testFunctionArgCount() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");
        
         print(doc);

        // let's check the argument count of "abs" function
        XMLAssert.assertXpathEvaluatesTo("1", "count(//fes:Function[@name=\"abs\"]/fes:Arguments/fes:Argument)", doc);
    }

    public void testTypeNameCount() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        XpathEngine xpath = XMLUnit.newXpathEngine();

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (!ft.isEnabled()) {
                it.remove();
            }
        }
        final int enabledCount = enabledTypes.size();

        assertEquals(enabledCount, xpath.getMatchingNodes(
                "/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType", doc).getLength());
    }

    public void testTypeNames() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (ft.isEnabled()) {
                String prefixedName = ft.getPrefixedName();

                String xpathExpr = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/"
                        + "wfs:FeatureType/wfs:Name[text()=\"" + prefixedName + "\"]";

                XMLAssert.assertXpathExists(xpathExpr, doc);
            }
        }
    }

    public void testOperationsMetadata() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        assertEquals("WFS_Capabilities", doc.getDocumentElement().getLocalName());

        XMLAssert.assertXpathExists("//ows:Operation[@name='GetCapabilities']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DescribeFeatureType']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='GetFeature']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='LockFeature']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='GetFeatureWithLock']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='Transaction']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='ListStoredQueries']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DescribeStoredQueries']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='CreateStoredQuery']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DropStoredQuery']", doc);
    }

    public void testValidCapabilitiesDocument() throws Exception {
        InputStream in = get("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Parser p = new Parser(new WFSConfiguration());
        p.setValidating(true);
        p.validate(in);
        
        for (Exception e : (List<Exception>)p.getValidationErrors()) {
            System.out.println(e.getLocalizedMessage());
        }
        assertTrue(p.getValidationErrors().isEmpty());
    }
    
    public void testLayerQualified() throws Exception {
     // filter on an existing namespace
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wfs?service=WFS&version=2.0.0&request=getCapabilities");
        
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());

        //TODO: renable assertions when all operations implemented
        //assertEquals(7, xpath.getMatchingNodes("//ows:Get[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        //assertEquals(7, xpath.getMatchingNodes("//ows:Post[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        
        //TODO: test with a non existing workspace
    }
    
    public void testSOAP() throws Exception {
        String xml = 
           "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
                " <soap:Header/> " + 
                " <soap:Body>" +
                "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>" + 
                " </soap:Body> " + 
            "</soap:Envelope> "; 

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getOutputStreamContent().getBytes()));
        
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:WFS_Capabilities").getLength());
    }

    public void testAcceptVersions11() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&acceptversions=1.1.0,1.0.0");
        assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());
        assertEquals("1.1.0", dom.getDocumentElement().getAttribute("version"));
    }

    public void testAcceptVersions11WithVersion() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&version=2.0.0&acceptversions=1.1.0,1.0.0");
        assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());
        assertEquals("1.1.0", dom.getDocumentElement().getAttribute("version"));   
    }

    public void testAcceptFormats() throws Exception {
        ServletResponse response = getAsServletResponse("wfs?request=GetCapabilities&version=2.0.0");
        assertEquals("application/xml", response.getContentType());

        response = getAsServletResponse("wfs?request=GetCapabilities&version=2.0.0&acceptformats=text/xml");
        assertEquals("text/xml", response.getContentType());
    }
}
