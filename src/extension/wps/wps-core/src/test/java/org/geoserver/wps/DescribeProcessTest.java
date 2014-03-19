/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeProcessTest extends WPSTestSupport {

    @Test
    public void testGetBuffer() throws Exception { // Standard Test A.4.3.1
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=JTS:buffer");
        // print(d);
        testBufferDescription(d);
    }
    
    @Test
    public void testPostBuffer() throws Exception { // Standard Test A.4.3.2
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
        		"<DescribeProcess xmlns=\"http://www.opengis.net/wps/1.0.0\" " +
        		"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
        		"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
        		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + 
        		"    <ows:Identifier>JTS:buffer</ows:Identifier>\r\n" + 
        		"</DescribeProcess>";
        Document d = postAsDOM(root(), request);
        // print(d);
        testBufferDescription(d);
    }

    @Test
    public void testGetBufferFeatureCollection() throws Exception { // Standard Test A.4.3.1
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gs:BufferFeatureCollection");
        // print(d);
    }
    
    private void testBufferDescription(Document d) throws Exception { // Standard Test A.4.3.3
        // first off, let's check it's schema compliant 
        checkValidationErrors(d);
        assertXpathExists( "/wps:ProcessDescriptions", d );
        
        assertXpathEvaluatesTo("true", "//ProcessDescription/@storeSupported", d);
        assertXpathEvaluatesTo("true", "//ProcessDescription/@statusSupported", d);
        
        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";
        
        // check store and status
        
        
        //first parameter
        assertXpathExists( base + "/Input[1]" , d );
        assertXpathExists( base + "/Input[1]/ComplexData", d );
        
        assertXpathEvaluatesTo("text/xml; subtype=gml/3.1.1", 
        		base + "/Input[1]/ComplexData/Default/Format/MimeType/child::text()", d);
        assertXpathEvaluatesTo("text/xml; subtype=gml/3.1.1", 
        		base + "/Input[1]/ComplexData/Supported/Format[1]/MimeType/child::text()", d);
        assertXpathEvaluatesTo("text/xml; subtype=gml/2.1.2", 
        		base + "/Input[1]/ComplexData/Supported/Format[2]/MimeType/child::text()", d);
        assertXpathEvaluatesTo("application/wkt", 
        		base + "/Input[1]/ComplexData/Supported/Format[3]/MimeType/child::text()", d);
        
        //second parameter
        assertXpathExists( base + "/Input[2]", d );
        assertXpathEvaluatesTo("distance", base + "/Input[2]/ows:Identifier/child::text()", d );
        assertXpathExists( base + "/Input[2]/LiteralData", d );

        assertXpathEvaluatesTo("xs:double", base + "/Input[2]/LiteralData/ows:DataType/child::text()", d );

        //output
        base = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        assertXpathExists( base + "/Output", d );
        assertXpathExists( base + "/Output/ComplexOutput", d );
    }
    
    /**
     * Tests encoding of bounding box outputs
     * @throws Exception
     */
    @Test
    public void testBounds() throws Exception {
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gs:Bounds");
        // print(d);
        checkValidationErrors(d);
        assertXpathEvaluatesTo("EPSG:4326", "//Output[ows:Identifier='bounds']/BoundingBoxOutput/Default/CRS", d);
        assertXpathEvaluatesTo("EPSG:4326", "//Output[ows:Identifier='bounds']/BoundingBoxOutput/Supported/CRS", d);
    }
    
    /* TODO Language Negotiation tests
    public void testGetLanguageGood() throws Exception { // Standard Test A.4.3.4
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gt:buffer&language=en-US" );
        print(d);
        testBufferDescription(d);
    }
    
    public void testGetLanguageBad() throws Exception { // Standard Test A.4.3.4
        Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=gt:buffer&language=zz-ZZ" );
        print(d);
        testBufferDescription(d);
    }

    public void testPostLanguageGod() throws Exception { // Standard Test A.4.3.4
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
    		"<DescribeProcess xmlns=\"http://www.opengis.net/wps/1.0.0\" " +
    		"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
    		"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
    		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    		"language=\"en-US\">\r\n" + 
    		"    <ows:Identifier>gt:buffer</ows:Identifier>\r\n" + 
    		"</DescribeProcess>";
        Document d = postAsDOM(root(), request);
        testBufferDescription(d);
    }

    public void testPostLanguageBad() throws Exception { // Standard Test A.4.3.4
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
			"<DescribeProcess xmlns=\"http://www.opengis.net/wps/1.0.0\" " +
			"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
			"xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
			"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
			"language=\"zz-ZZ\">\r\n" + 
			"    <ows:Identifier>gt:buffer</ows:Identifier>\r\n" + 
			"</DescribeProcess>";
        Document d = postAsDOM(root(), request);
        testBufferDescription(d);
    }
    */
}
