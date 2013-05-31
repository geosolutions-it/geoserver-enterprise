package org.geoserver.wfs.xslt.rest;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class TransformRestTest extends GeoServerTestSupport {

    private XpathEngine xpath;
    private TransformRepository repository; 

    @Override
    protected void setUpInternal() throws Exception {
        xpath = XMLUnit.newXpathEngine();
        repository = (TransformRepository) applicationContext.getBean("transformRepository");
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        File dd = dataDirectory.getDataDirectoryRoot();
        File wfs = new File(dd, "wfs");
        File transform = new File(wfs, "transform");
        if (transform.exists()) {
            FileUtils.deleteDirectory(transform);
        }
        assertTrue(transform.mkdirs());
        FileUtils.copyDirectory(new File("src/test/resources/org/geoserver/wfs/xslt"), transform);
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("h", "http://www.w3.org/1999/xhtml");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }


    public void testListHTML() throws Exception {
        Document d = getAsDOM("rest/services/wfs/transforms");
        // print(d);
        
        assertEquals("1", xpath.evaluate("count(//h:h2)", d));
        assertEquals("XSLT transformations:", xpath.evaluate("/h:html/h:body/h:h2", d));
        assertEquals("http://localhost/geoserver/rest/services/wfs/transforms/general.html", 
                xpath.evaluate(("//h:li[h:a = 'general']/h:a/@href"), d));
    }
    

    public void testListXML() throws Exception {
        Document d = getAsDOM("rest/services/wfs/transforms.xml");
        // print(d);
        
        assertEquals("3", xpath.evaluate("count(//transform)", d));
        assertEquals("http://localhost/geoserver/rest/services/wfs/transforms/general.xml", 
                xpath.evaluate("//transform[name='general']/atom:link/@href", d));
    }
    
 
    public void testGetTransformHTML() throws Exception {
        Document d = getAsDOM("rest/services/wfs/transforms/general");
        // print(d);
        
        assertEquals("Source format: \"text/xml; subtype=gml/2.1.2\"", xpath.evaluate("//h:li[1]", d));
        assertEquals("Output format: \"text/html; subtype=xslt\"", xpath.evaluate("//h:li[2]", d));
        assertEquals("File extension: \"html\"", xpath.evaluate("//h:li[3]", d));
        assertEquals("XSLT transform: \"general.xslt\"", xpath.evaluate("//h:li[4]", d));
    }
    
 
    public void testGetTransformXML() throws Exception {
        Document d = getAsDOM("rest/services/wfs/transforms/general.xml");
        // print(d);
        
        assertEquals("text/xml; subtype=gml/2.1.2", xpath.evaluate("//sourceFormat", d));
        assertEquals("text/html; subtype=xslt", xpath.evaluate("//outputFormat", d));
        assertEquals("html", xpath.evaluate("//fileExtension", d));
        assertEquals("general.xslt", xpath.evaluate("//xslt", d));
    }
    

    public void testPostXML() throws Exception {
        String xml = "<transform>\n" +
                "  <name>buildings</name>\n" + //
        		"  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n" + // 
        		"  <outputFormat>text/html</outputFormat>\n" + //
        		"  <fileExtension>html</fileExtension>\n" + //
        		"  <xslt>buildings.xslt</xslt>\n" + //
        		"  <featureType>\n" + //
        		"    <name>cite:Buildings</name>\n" + // 
        		"  </featureType>\n" + 
        		"</transform>\n";
        MockHttpServletResponse response = postAsServletResponse("rest/services/wfs/transforms", xml);
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/rest/services/wfs/transforms/buildings" ) );
        
        TransformInfo info = repository.getTransformInfo("buildings");
        assertNotNull(info);
    }
    

    public void testPostXSLT() throws Exception {
        String xslt = FileUtils.readFileToString(new File("src/test/resources/org/geoserver/wfs/xslt/general2.xslt"));
        
        // test for missing params
        MockHttpServletResponse response = postAsServletResponse("rest/services/wfs/transforms?name=general2", xslt, "application/xslt+xml");
        assertEquals(400, response.getStatusCode());
        
        // now pass all
        response = postAsServletResponse("rest/services/wfs/transforms?name=general2&sourceFormat=gml&outputFormat=HTML&outputMimeType=text/html", xslt, "application/xslt+xml");
        assertEquals(201, response.getStatusCode());
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/rest/services/wfs/transforms/general2" ) );

        TransformInfo info = repository.getTransformInfo("general2");
        assertNotNull(info);
        assertEquals("gml", info.getSourceFormat());
        assertEquals("HTML", info.getOutputFormat());
        assertEquals("text/html", info.getOutputMimeType());
    }
    

    public void testPutXML() throws Exception {
        // let's change the output format
        String xml = "<transform>\n" + 
        		"  <sourceFormat>text/xml; subtype=gml/2.1.2</sourceFormat>\n" + 
        		"  <outputFormat>text/html</outputFormat>\n" + 
        		"  <fileExtension>html</fileExtension>\n" + 
        		"  <xslt>general.xslt</xslt>\n" + 
        		"</transform>";
        
        MockHttpServletResponse response = putAsServletResponse("rest/services/wfs/transforms/general", xml, "text/xml");
        assertEquals(200, response.getStatusCode());
        
        TransformInfo info = repository.getTransformInfo("general");
        assertEquals("text/html", info.getOutputFormat());
    }
    

    public void testPutXSLT() throws Exception {
        String xslt = FileUtils.readFileToString(new File("src/test/resources/org/geoserver/wfs/xslt/general2.xslt"));
        MockHttpServletResponse response = putAsServletResponse("rest/services/wfs/transforms/general", xslt, "application/xslt+xml");
        assertEquals(200, response.getStatusCode());
        
        TransformInfo info = repository.getTransformInfo("general");
        InputStream is = null;
        String actual = null;
        try {
            is = repository.getTransformSheet(info);
            actual = IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
        assertEquals(xslt, actual);
    }
    

    public void testDelete() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("rest/services/wfs/transforms/general");
        assertEquals(200, response.getStatusCode());
        
        TransformInfo info = repository.getTransformInfo("general");
        assertNull(info);
    }
    
}
