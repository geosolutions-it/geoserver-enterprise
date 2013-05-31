package org.geoserver.wfs.response;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Test;
import junit.framework.TestResult;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class Ogr2OgrWfsTest extends GeoServerTestSupport {
    
    public static Test suite() {
        OgrConfiguration.DEFAULT.ogr2ogrLocation = Ogr2OgrTestUtil.getOgr2Ogr();
        OgrConfiguration.DEFAULT.gdalData = Ogr2OgrTestUtil.getGdalData();
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("", "http://www.opengis.net/wfs");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        
        return new OneTimeTestSetup(new Ogr2OgrWfsTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    }
    
    @Override
    public void run(TestResult result) {
        if (!Ogr2OgrTestUtil.isOgrAvailable())
            System.out.println("Skipping ogr2ogr wfs tests, ogr2ogr could not be found, " + getName());
        else
            super.run(result);
        
        // force reload of the config, some tests alter it
        Ogr2OgrConfigurator configurator = applicationContext.getBean(Ogr2OgrConfigurator.class);
        configurator.loadConfiguration();
    }
    
    public void testCapabilities() throws Exception {
        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);
        
        // while we cannot know what formats are available, the other tests won't pass if KML is not there 
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
    }
    
    public void testEmptyCapabilities() throws Exception {
        Ogr2OgrOutputFormat of = applicationContext.getBean(Ogr2OgrOutputFormat.class);
        of.clearFormats();
        
        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);
        
        // this used to NPE 
        assertXpathEvaluatesTo("0", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:SHAPE-ZIP)", dom);
    }

    public void testSimpleRequest() throws Exception {
        String request = "wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);
        
        // check content type
        assertEquals("application/vnd.google-earth.kml", resp.getContentType());
        
        // read back
        Document dom = dom(getBinaryInputStream(resp));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }
    
    public void testDoubleRequest() throws Exception {
        String request = "wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) 
            + "," + getLayerId(MockData.BRIDGES) + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);
        
        // check content type
        assertEquals("application/zip", resp.getContentType());
        
        // check content disposition
        assertEquals("attachment; filename=Buildings.zip", resp.getHeader("Content-Disposition"));
        
        // read back
        ZipInputStream zis = new ZipInputStream(getBinaryInputStream(resp));

        // get buildings entry
        ZipEntry entry = null;
        entry = zis.getNextEntry();
        while (entry != null) {
            if (entry.getName().equals("Buildings.kml")) {
                break;
            }
            entry = zis.getNextEntry();
        }
        
        assertNotNull(entry);
        assertEquals("Buildings.kml", entry.getName());

        // parse the kml to check it's really xml... 
        Document dom = dom(zis);
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }
}
