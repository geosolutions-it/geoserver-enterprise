/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.net.URL;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geotools.data.DataUtilities;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CoverageTest extends CatalogRESTTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory)
            throws Exception {
        dataDirectory.addWellKnownCoverageTypes();
    }
    
    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coverages.xml");
        assertEquals( 
            catalog.getCoveragesByNamespace( catalog.getNamespaceByPrefix( "wcs") ).size(), 
            dom.getElementsByTagName( "coverage").getLength() );
    }
    
    void addCoverageStore(boolean autoConfigureCoverage) throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip)  );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/usaWorldImage/file.worldimage" + 
                (!autoConfigureCoverage ? "?configure=none" : ""), bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
    }
    
    public void testGetAllByCoverageStore() throws Exception {
        addCoverageStore(true);
        Document dom = getAsDOM( "/rest/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals( 1, dom.getElementsByTagName( "coverage").getLength() );
        assertXpathEvaluatesTo( "1", "count(//coverage/name[text()='usa'])", dom );
    }
    
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble/coverages").getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble/coverages").getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.xml");
        
        assertXpathEvaluatesTo("BlueMarble", "/coverage/name", dom);
        assertXpathEvaluatesTo( "1", "count(//latLonBoundingBox)", dom);
        assertXpathEvaluatesTo( "1", "count(//nativeFormat)", dom);
        assertXpathEvaluatesTo( "1", "count(//grid)", dom);
        assertXpathEvaluatesTo( "1", "count(//supportedFormats)", dom);
    }
    
  
  public void testGetAsJSON() throws Exception {
      JSON json = getAsJSON( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.json");
      JSONObject coverage = ((JSONObject)json).getJSONObject("coverage");
      assertNotNull(coverage);
      
      assertEquals( "BlueMarble", coverage.get("name") );
      
  }
  
  public void testGetAsHTML() throws Exception {
      Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.html" );
      assertEquals( "html", dom.getDocumentElement().getNodeName() );
  }

  public void testPostAsXML() throws Exception {
        String req = "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:usa" +
            "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff" +
            "&gridbasecrs=EPSG:4326&store=true";
        
        Document dom = getAsDOM( req );
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        addCoverageStore(false);
        dom = getAsDOM( "/rest/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals( 0, dom.getElementsByTagName( "coverage").getLength() );
        
        String xml = 
            "<coverage>" +
                "<name>usa</name>"+
                "<title>usa is a A raster file accompanied by a spatial data file</title>" + 
                "<description>Generated from WorldImage</description>" + 
                "<srs>EPSG:4326</srs>" +
                /*"<latLonBoundingBox>"+
                  "<minx>-130.85168</minx>"+
                  "<maxx>-62.0054</maxx>"+
                  "<miny>20.7052</miny>"+
                  "<maxy>54.1141</maxy>"+
                "</latLonBoundingBox>"+
                "<nativeBoundingBox>"+
                  "<minx>-130.85168</minx>"+
                  "<maxx>-62.0054</maxx>"+
                  "<miny>20.7052</miny>"+
                  "<maxy>54.1141</maxy>"+
                  "<crs>EPSG:4326</crs>"+
                "</nativeBoundingBox>"+
                "<grid dimension=\"2\">"+
                    "<range>"+
                      "<low>0 0</low>"+
                      "<high>983 598</high>"+
                    "</range>"+
                    "<transform>"+
                      "<scaleX>0.07003690742624616</scaleX>"+
                      "<scaleY>-0.05586772575250837</scaleY>"+
                      "<shearX>0.0</shearX>"+
                      "<shearX>0.0</shearX>"+
                      "<translateX>-130.81666154628687</translateX>"+
                      "<translateY>54.08616613712375</translateY>"+
                    "</transform>"+
                    "<crs>EPSG:4326</crs>"+
                "</grid>"+*/
                "<supportedFormats>"+
                  "<string>PNG</string>"+
                  "<string>TIFF</string>"+
                "</supportedFormats>"+
                "<requestSRS>"+
                  "<string>EPSG:4326</string>"+
                "</requestSRS>"+
                "<responseSRS>"+
                  "<string>EPSG:4326</string>"+
                "</responseSRS>"+
                "<store>usaWorldImage</store>"+
                "<namespace>gs</namespace>"+
              "</coverage>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/coveragestores/usaWorldImage/coverages/", xml, "text/xml");
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/coveragestores/usaWorldImage/coverages/usa" ) );

        dom = getAsDOM( req );
        assertEquals( "wcs:Coverages", dom.getDocumentElement().getNodeName() );

        dom = getAsDOM("/rest/workspaces/gs/coveragestores/usaWorldImage/coverages/usa.xml");
        assertXpathEvaluatesTo("-130.85168", "/coverage/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo("983 598", "/coverage/grid/range/high", dom);
    }

    public void testPutWithCalculation() throws Exception {
        String path = "/rest/workspaces/wcs/coveragestores/DEM/coverages/DEM.xml";
        String clearLatLonBoundingBox =
                "<coverage>"
                + "<latLonBoundingBox/>" 
              + "</coverage>";

        MockHttpServletResponse response =
                putAsServletResponse(path, clearLatLonBoundingBox, "text/xml");
        assertEquals(
                "Couldn't remove lat/lon bounding box: \n" + response.getOutputStreamContent(),
                200,
                response.getStatusCode());
        
        Document dom = getAsDOM(path);
        assertXpathEvaluatesTo("0.0", "/coverage/latLonBoundingBox/minx", dom);
        print(dom);
        
        String updateNativeBounds =
                "<coverage>" 
                + "<srs>EPSG:3785</srs>"
              + "</coverage>";

        response = putAsServletResponse(
                path,
                updateNativeBounds,
                "text/xml");

        assertEquals(
                "Couldn't update native bounding box: \n"
                        + response.getOutputStreamContent(), 200,
                response.getStatusCode());
        dom = getAsDOM(path);
        print(dom);
        assertXpathExists("/coverage/latLonBoundingBox/minx[text()!='0.0']",
                dom);
    }

//    public void testPostAsJSON() throws Exception {
//        Document dom = getAsDOM( "wfs?request=getfeature&typename=wcs:pdsa");
//        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
//        
//        addPropertyDataStore(false);
//        String json = 
//          "{" + 
//           "'coverage':{" + 
//              "'name':'pdsa'," +
//              "'nativeName':'pdsa'," +
//              "'srs':'EPSG:4326'," +
//              "'nativeBoundingBox':{" +
//                 "'minx':0.0," +
//                 "'maxx':1.0," +
//                 "'miny':0.0," +
//                 "'maxy':1.0," +
//                 "'crs':'EPSG:4326'" +
//              "}," +
//              "'nativeCRS':'EPSG:4326'," +
//              "'store':'pds'" +
//             "}" +
//          "}";
//        MockHttpServletResponse response =  
//            postAsServletResponse( "/rest/workspaces/gs/coveragestores/pds/coverages/", json, "text/json");
//        
//        assertEquals( 201, response.getStatusCode() );
//        assertNotNull( response.getHeader( "Location") );
//        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/coveragestores/pds/coverages/pdsa" ) );
//        
//        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
//        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
//        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
//    }
//    
    public void testPostToResource() throws Exception {
        String xml = 
            "<coverage>"+
              "<name>foo</name>"+
            "</coverage>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble", xml, "text/xml");
        assertEquals( 405, response.getStatusCode() );
    }

    public void testPut() throws Exception {
        String xml = 
          "<coverage>" + 
            "<title>new title</title>" +  
          "</coverage>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        Document dom = getAsDOM("/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble.xml");
        assertXpathEvaluatesTo("new title", "/coverage/title", dom );
        
        CoverageInfo c = catalog.getCoverageByName( "wcs", "BlueMarble");
        assertEquals( "new title", c.getTitle() );
    }
    
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<coverage>" + 
              "<title>new title</title>" +  
            "</coverage>";
          MockHttpServletResponse response = 
              putAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/NonExistant", xml, "text/xml");
          assertEquals( 404, response.getStatusCode() );
    }
   
    public void testDelete() throws Exception {
        assertNotNull( catalog.getCoverageByName("wcs", "BlueMarble"));
        for (LayerInfo l : catalog.getLayers( catalog.getCoverageByName("wcs", "BlueMarble") ) ) {
            catalog.remove(l);
        }
        assertEquals( 200,  
            deleteAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble").getStatusCode());
        assertNull( catalog.getCoverageByName("wcs", "BlueMarble"));
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404,  
            deleteAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/NonExistant").getStatusCode());
    }
    
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getCoverageByName("wcs", "BlueMarble"));
        assertNotNull(catalog.getLayerByName("wcs:BlueMarble"));
        
        assertEquals(403, deleteAsServletResponse( 
            "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble").getStatusCode());
        assertEquals( 200, deleteAsServletResponse( 
            "/rest/workspaces/wcs/coveragestores/BlueMarble/coverages/BlueMarble?recurse=true").getStatusCode());

        assertNull(catalog.getCoverageByName("wcs", "BlueMarble"));
        assertNull(catalog.getLayerByName("wcs:BlueMarble"));
    }
}
