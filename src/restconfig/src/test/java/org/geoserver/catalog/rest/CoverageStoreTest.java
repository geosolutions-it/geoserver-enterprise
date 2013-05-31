/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CoverageStoreTest extends CatalogRESTTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory)
            throws Exception {
        dataDirectory.addWellKnownCoverageTypes();
    }
    
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores.xml");
        assertEquals( catalog.getStoresByWorkspace( "wcs", CoverageStoreInfo.class ).size(), 
            dom.getElementsByTagName( "coverageStore").getLength() );
    }
    
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/wcs/coveragestores.json");
        assertTrue( json instanceof JSONObject );
        
        Object coveragestores = ((JSONObject)json).getJSONObject("coverageStores").get("coverageStore");
        assertNotNull( coveragestores );
        
        if( coveragestores instanceof JSONArray ) {
            assertEquals( catalog.getCoverageStoresByWorkspace("wcs").size() , ((JSONArray)coveragestores).size() );    
        }
        else {
            assertEquals( 1, catalog.getCoverageStoresByWorkspace("wcs").size() );
        }
    }
    
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores.html");
        List<CoverageStoreInfo> coveragestores = catalog.getCoverageStoresByWorkspace("wcs"); 
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( coveragestores.size(), links.getLength() );
        
        for ( int i = 0; i < coveragestores.size(); i++ ){
            CoverageStoreInfo cs = coveragestores.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( cs.getName() + ".html") );
        }
    }
    
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/wcs/coveragestores").getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/wcs/coveragestores").getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertEquals( "coverageStore", dom.getDocumentElement().getNodeName() );
        assertEquals( "BlueMarble", xp.evaluate( "/coverageStore/name", dom) );
        assertEquals( "wcs", xp.evaluate( "/coverageStore/workspace/name", dom) );
    }
    
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble.html");
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "wcs", "BlueMarble" );
        List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore( cs );
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( coverages.size(), links.getLength() );
        
        for ( int i = 0; i < coverages.size(); i++ ){
            CoverageInfo ft = coverages.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( ft.getName() + ".html") );
        }
    }
    
    File setupNewCoverageStore() throws Exception {
        File dir = new File( "./target/usa" );
        dir.mkdir();
        dir.deleteOnExit();
        
        File f = new File( dir, "usa.prj");
        f.deleteOnExit();
        FileOutputStream fout = new FileOutputStream( f );
        IOUtils.copy( getClass().getResourceAsStream("test-data/usa.prj"), fout );
        fout.flush();
        fout.close();
       
        f = new File( dir, "usa.meta");
        f.deleteOnExit();
        fout = new FileOutputStream( f ); 
        IOUtils.copy( getClass().getResourceAsStream("test-data/usa.meta"), fout );
        fout.flush();
        fout.close();
        
        f = new File( dir, "usa.png");
        f.deleteOnExit();
        
        fout = new FileOutputStream( f ); 
        IOUtils.copy( getClass().getResourceAsStream("test-data/usa.png"), fout );
        fout.flush();
        fout.close();
        
        return f;
    }
    
    public void testPostAsXML() throws Exception {
        
        File f = setupNewCoverageStore();
        String xml =
            "<coverageStore>" +
              "<name>newCoverageStore</name>" +
              "<type>WorldImage</type>" +
              "<url>file://" + f.getAbsolutePath() + "</url>" + 
              "<workspace>wcs</workspace>" + 
            "</coverageStore>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/wcs/coveragestores", xml, "text/xml" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/wcs/coveragestores/newCoverageStore" ) );

        CoverageStoreInfo newCoverageStore = catalog.getCoverageStoreByName( "newCoverageStore" );
        assertNotNull( newCoverageStore );
        
        assertNotNull(newCoverageStore.getFormat());
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/wcs/coveragestores/BlueMarble.json" );
        
        JSONObject coverageStore = ((JSONObject)json).getJSONObject("coverageStore");
        assertNotNull(coverageStore);
        
        assertEquals( "BlueMarble", coverageStore.get( "name") );
        assertEquals( "wcs", coverageStore.getJSONObject( "workspace").get( "name" ));
        assertNotNull( coverageStore.get( "type") );
        assertNotNull( coverageStore.get( "url") );
    }
    
    public void testPostAsJSON() throws Exception {
        File f = setupNewCoverageStore();
        String json = 
            "{'coverageStore':{" +
                "'name':'newCoverageStore'," +
                "'type': 'WorldImage'," + 
                "'url':'" + f.getAbsolutePath().replace('\\','/')  + "'," +
                "'workspace':'wcs'," +
              "}" +
            "}";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/wcs/coveragestores", json, "text/json" );
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/wcs/coveragestores/newCoverageStore" ) );
        
        CoverageStoreInfo newCoverageStore = catalog.getCoverageStoreByName( "newCoverageStore" );
        assertNotNull( newCoverageStore );
        assertNotNull( newCoverageStore.getFormat() );
    }
    
    public void testPostToResource() throws Exception {
        String xml = 
        "<coverageStore>" + 
         "<name>BlueMarble</name>" + 
         "<enabled>false</enabled>" + 
        "</coverageStore>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble", xml, "text/xml");
        assertEquals( 405, response.getStatusCode() );
    }
    
    public void testPut() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("true", "/coverageStore/enabled", dom );
        
        String xml = 
        "<coverageStore>" + 
         "<name>BlueMarble</name>" + 
         "<enabled>false</enabled>" + 
        "</coverageStore>";
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );

        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("false", "/coverageStore/enabled", dom );
        
        assertFalse( catalog.getCoverageStoreByName( "wcs", "BlueMarble").isEnabled() );
    }
    
    public void testPut2() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/BlueMarble.xml");
        assertXpathEvaluatesTo("GeoTIFF", "/coverageStore/type", dom );
        
        String xml = 
        "<coverageStore>" + 
         "<name>BlueMarble</name>" + 
         "<type>WorldImage</type>" + 
         "</coverageStore>";
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/wcs/coveragestores/BlueMarble", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName( "wcs", "BlueMarble" );
        assertEquals( "WorldImage", cs.getType() );
    }
    
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<coverageStore>" + 
            "<name>changed</name>" + 
            "</coverageStore>";

        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/wcs/coveragestores/nonExistant", xml, "text/xml" );
        assertEquals( 404, response.getStatusCode() );
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/nonExistant").getStatusCode() );
    }
    
    public void testDelete() throws Exception {
        CoverageStoreInfo cs = catalog.getCoverageStoreByName("wcs","BlueMarble");
        List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore(cs);
        for ( CoverageInfo c : coverages ) {
            for ( LayerInfo l : catalog.getLayers(c) ) {
                catalog.remove(l);
            }
            catalog.remove( c );
        }
        
        assertEquals( 200, deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble").getStatusCode());
        assertNull( catalog.getCoverageStoreByName("wcs", "BlueMarble"));
    }
    
    public void testDeleteNonEmpty() throws Exception {
        assertEquals( 401, deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble").getStatusCode());
    }
    
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getCoverageStoreByName("wcs", "BlueMarble"));
        MockHttpServletResponse response =
            deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/BlueMarble?recurse=true");
        assertEquals(200, response.getStatusCode());

        assertNull(catalog.getCoverageStoreByName("wcs", "BlueMarble"));
        
        for (CoverageInfo c : catalog.getCoverages()) {
            if (c.getStore().getName().equals("BlueMarble")) {
                fail();
            }
        }
    }
}
