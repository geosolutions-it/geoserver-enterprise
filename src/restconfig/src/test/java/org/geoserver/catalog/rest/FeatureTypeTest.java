/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.MultiPolygon;

public class FeatureTypeTest extends CatalogRESTTestSupport {

    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/featuretypes.xml");
        assertEquals( 
            catalog.getFeatureTypesByNamespace( catalog.getNamespaceByPrefix( "sf") ).size(), 
            dom.getElementsByTagName( "featureType").getLength() );
    }
    
    void addPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( zbytes );
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( bytes ) );
        writer.write( "_=name:String,pointProperty:Point\n" );
        writer.write( "pdsa.0='zero'|POINT(0 0)\n");
        writer.write( "pdsa.1='one'|POINT(1 1)\n");
        writer.flush();
        
        zout.putNextEntry( new ZipEntry( "pdsa.properties") );
        zout.write( bytes.toByteArray() );
        bytes.reset();
        
        writer.write( "_=name:String,pointProperty:Point\n" );
        writer.write( "pdsb.0='two'|POINT(2 2)\n");
        writer.write( "pdsb.1='trhee'|POINT(3 3)\n");
        writer.flush();
        zout.putNextEntry( new ZipEntry( "pdsb.properties" ) );
        zout.write( bytes.toByteArray() );
        
        zout.flush();
        zout.close();
        
        String q = "configure=" + (configureFeatureType ? "all" : "none"); 
        put( "/rest/workspaces/gs/datastores/pds/file.properties?" + q, zbytes.toByteArray(), "application/zip");
    }
    
    void addGeomlessPropertyDataStore(boolean configureFeatureType) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( zbytes );
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( bytes ) );
        writer.write( "_=name:String,intProperty:Integer\n" );
        writer.write( "ngpdsa.0='zero'|0\n");
        writer.write( "ngpdsa.1='one'|1\n");
        writer.flush();
        
        zout.putNextEntry( new ZipEntry( "ngpdsa.properties") );
        zout.write( bytes.toByteArray() );
        bytes.reset();
        
        writer.write( "_=name:String,intProperty:Integer\n" );
        writer.write( "ngpdsb.0='two'|2\n");
        writer.write( "ngpdsb.1='trhee'|3\n");
        writer.flush();
        zout.putNextEntry( new ZipEntry( "ngpdsb.properties" ) );
        zout.write( bytes.toByteArray() );
        
        zout.flush();
        zout.close();
        
        String q = "configure=" + (configureFeatureType ? "all" : "none"); 
        put( "/rest/workspaces/gs/datastores/ngpds/file.properties?" + q, zbytes.toByteArray(), "application/zip");
    }
    
    public void testGetAllByDataStore() throws Exception {
      
        addPropertyDataStore(true);
        
        Document dom = getAsDOM( "/rest/workspaces/gs/datastores/pds/featuretypes.xml");
        
        assertEquals( 2, dom.getElementsByTagName( "featureType").getLength() );
        assertXpathEvaluatesTo( "1", "count(//featureType/name[text()='pdsa'])", dom );
        assertXpathEvaluatesTo( "1", "count(//featureType/name[text()='pdsb'])", dom );
    }
    
    public void testGetAllAvailable() throws Exception {
        addPropertyDataStore(false);
        
        Document dom = getAsDOM( "/rest/workspaces/gs/datastores/pds/featuretypes.xml?list=available");
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsa'])", dom);
        assertXpathEvaluatesTo("1", "count(//featureTypeName[text()='pdsb'])", dom);
    }

    public void testGetAllAvailableWithGeometryOnly() throws Exception {
        addGeomlessPropertyDataStore(false);

        Document dom = getAsDOM( "/rest/workspaces/gs/datastores/ngpds/featuretypes.xml?list=available");
        assertXpathEvaluatesTo("2", "count(//featureTypeName)", dom);

        dom = getAsDOM( "/rest/workspaces/gs/datastores/ngpds/featuretypes.xml?list=available_with_geom");
        assertXpathEvaluatesTo("0", "count(//featureTypeName)", dom);
    }

    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes").getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes").getStatusCode() );
    }
    
    public void testPostAsXML() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&typename=sf:pdsa");
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        addPropertyDataStore(false);
        String xml = 
          "<featureType>"+
            "<name>pdsa</name>"+
            "<nativeName>pdsa</nativeName>"+
            "<srs>EPSG:4326</srs>" + 
            "<nativeCRS>EPSG:4326</nativeCRS>" + 
            "<nativeBoundingBox>"+
              "<minx>0.0</minx>"+
              "<maxx>1.0</maxx>"+
              "<miny>0.0</miny>"+
              "<maxy>1.0</maxy>"+
              "<crs>EPSG:4326</crs>" + 
            "</nativeBoundingBox>"+
            "<store>pds</store>" + 
          "</featureType>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/", xml, "text/xml");
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/pds/featuretypes/pdsa" ) );
        
        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
    }
    
    public void testPostAsJSON() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&typename=sf:pdsa");
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        addPropertyDataStore(false);
        String json = 
          "{" + 
           "'featureType':{" + 
              "'name':'pdsa'," +
              "'nativeName':'pdsa'," +
              "'srs':'EPSG:4326'," +
              "'nativeBoundingBox':{" +
                 "'minx':0.0," +
                 "'maxx':1.0," +
                 "'miny':0.0," +
                 "'maxy':1.0," +
                 "'crs':'EPSG:4326'" +
              "}," +
              "'nativeCRS':'EPSG:4326'," +
              "'store':'pds'" +
             "}" +
          "}";
        MockHttpServletResponse response =  
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/", json, "text/json");
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/pds/featuretypes/pdsa" ) );
        
        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pdsa");
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals( 2, dom.getElementsByTagName( "gs:pdsa").getLength());
    }
    
    public void testPostToResource() throws Exception {
        addPropertyDataStore(true);
        String xml = 
            "<featureType>"+
              "<name>pdsa</name>"+
            "</featureType>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes/pdsa", xml, "text/xml");
        assertEquals( 405, response.getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/featuretypes/PrimitiveGeoFeature.xml");
        
        assertEquals( "featureType", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("PrimitiveGeoFeature", "/featureType/name", dom);
        assertXpathEvaluatesTo( "EPSG:4326", "/featureType/srs", dom);
        assertEquals( CRS.decode( "EPSG:4326" ).toWKT(), xp.evaluate( "/featureType/nativeCRS", dom ) );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "sf", "PrimitiveGeoFeature" );
        
        /*
        ReferencedEnvelope re = ft.getNativeBoundingBox();
        assertXpathEvaluatesTo(  re.getMinX()+"" , "/featureType/nativeBoundingBox/minx", dom );
        assertXpathEvaluatesTo(  re.getMaxX()+"" , "/featureType/nativeBoundingBox/maxx", dom );
        assertXpathEvaluatesTo(  re.getMinY()+"" , "/featureType/nativeBoundingBox/miny", dom );
        assertXpathEvaluatesTo(  re.getMaxY()+"" , "/featureType/nativeBoundingBox/maxy", dom );
        */
        ReferencedEnvelope re = ft.getLatLonBoundingBox();
        assertXpathEvaluatesTo(  re.getMinX()+"" , "/featureType/latLonBoundingBox/minx", dom );
        assertXpathEvaluatesTo(  re.getMaxX()+"" , "/featureType/latLonBoundingBox/maxx", dom );
        assertXpathEvaluatesTo(  re.getMinY()+"" , "/featureType/latLonBoundingBox/miny", dom );
        assertXpathEvaluatesTo(  re.getMaxY()+"" , "/featureType/latLonBoundingBox/maxy", dom );
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/featuretypes/PrimitiveGeoFeature.json");
        JSONObject featureType = ((JSONObject)json).getJSONObject("featureType");
        assertNotNull(featureType);
        
        assertEquals( "PrimitiveGeoFeature", featureType.get("name") );
        assertEquals( CRS.decode("EPSG:4326").toWKT(), featureType.get( "nativeCRS") );
        assertEquals( "EPSG:4326", featureType.get( "srs") );
    }
    
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.html");
    }
    
    public void testPut() throws Exception {
        String xml = 
          "<featureType>" + 
            "<title>new title</title>" +  
          "</featureType>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        Document dom = getAsDOM("/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature.xml");
        assertXpathEvaluatesTo("new title", "/featureType/title", dom );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName( "sf", "PrimitiveGeoFeature");
        assertEquals( "new title", ft.getTitle() );
    }
    
    public void testPutWithCalculation() throws Exception {
        String clearLatLonBoundingBox =
              "<featureType>"
                + "<nativeBoundingBox>"
                  + "<minx>-180.0</minx>" + "<maxx>180.0</maxx>"
                  + "<miny>-90.0</miny>" + "<maxy>90.0</maxy>"
                  + "<crs>EPSG:4326</crs>" 
                + "</nativeBoundingBox>"
                + "<latLonBoundingBox/>"
            + "</featureType>";
        
        String path = "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature";
        MockHttpServletResponse response =
                putAsServletResponse(path, clearLatLonBoundingBox, "text/xml");
        assertEquals("Couldn't remove lat/lon bounding box:\n" + response.getOutputStreamContent(),
                200, response.getStatusCode());

        Document dom = getAsDOM(path + ".xml");
        assertXpathEvaluatesTo("0.0", "/featureType/latLonBoundingBox/minx", dom);
        
        String updateNativeBounds =
                "<featureType>"
                  + "<srs>EPSG:3785</srs>"
                  + "<nativeBoundingBox>"
                    + "<minx>-20037508.34</minx>"
                    + "<maxx>20037508.34</maxx>"
                    + "<miny>-20037508.34</miny>"
                    + "<maxy>20037508.34</maxy>"
                    + "<crs>EPSG:3785</crs>"
                  + "</nativeBoundingBox>"
              + "</featureType>";
                     
        response = putAsServletResponse(path + ".xml", updateNativeBounds, "text/xml");
        assertEquals("Couldn't update native bounding box: \n" + response.getOutputStreamContent(),
                200, response.getStatusCode());
        dom = getAsDOM(path + ".xml");
        print(dom);
        assertXpathExists("/featureType/latLonBoundingBox/minx[text()!='0.0']", dom);
    }

    public void testPutNonExistant() throws Exception {
        String xml = 
            "<featureType>" + 
              "<title>new title</title>" +  
            "</featureType>";
          MockHttpServletResponse response = 
              putAsServletResponse("/rest/workspaces/sf/datastores/sf/featuretypes/NonExistant", xml, "text/xml");
          assertEquals( 404, response.getStatusCode() );
    }
   
    public void testDelete() throws Exception {
        assertNotNull( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        for (LayerInfo l : catalog.getLayers( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature") ) ) {
            catalog.remove(l);
        }
        assertEquals( 200,  
            deleteAsServletResponse( "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature").getStatusCode());
        assertNull( catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404,  
            deleteAsServletResponse( "/rest/workspaces/sf/datastores/sf/featuretypes/NonExistant").getStatusCode());
    }
    
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        assertNotNull(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        
        assertEquals(403, deleteAsServletResponse( 
            "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature").getStatusCode());
        assertEquals( 200, deleteAsServletResponse( 
            "/rest/workspaces/sf/datastores/sf/featuretypes/PrimitiveGeoFeature?recurse=true").getStatusCode());

        assertNull(catalog.getFeatureTypeByName("sf", "PrimitiveGeoFeature"));
        assertNull(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
    }
    
    public void testPostGeometrylessFeatureType() throws Exception {
        addGeomlessPropertyDataStore(false);
        
        String xml = 
            "<featureType>" + 
              "<name>ngpdsa</name>" +
            "</featureType>";
        
      MockHttpServletResponse response = 
          postAsServletResponse("/rest/workspaces/gs/datastores/ngpds/featuretypes", xml, "text/xml");
      assertEquals( 201, response.getStatusCode() );
      assertNotNull( response.getHeader( "Location") );
      assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/datastores/ngpds/featuretypes/ngpdsa" ) );
    }
    
    public void testCreateFeatureType() throws Exception {
        String xml = "<featureType>\n" + 
        		"  <name>states</name>\n" + 
        		"  <nativeName>states</nativeName>\n" + 
        		"  <namespace>\n" + 
        		"    <name>cite</name>\n" + 
        		"  </namespace>\n" + 
        		"  <title>USA Population</title>\n" + 
        		"  <srs>EPSG:4326</srs>\n" + 
        		"  <attributes>\n" + 
        		"    <attribute>\n" + 
        		"      <name>the_geom</name>\n" + 
        		"      <binding>com.vividsolutions.jts.geom.MultiPolygon</binding>\n" + 
        		"    </attribute>\n" + 
        		"    <attribute>\n" + 
        		"      <name>STATE_NAME</name>\n" + 
        		"      <binding>java.lang.String</binding>\n" + 
        		"      <length>25</length>\n" + 
        		"    </attribute>\n" + 
        		"    <attribute>\n" + 
        		"      <name>LAND_KM</name>\n" + 
        		"      <binding>java.lang.Double</binding>\n" + 
        		"    </attribute>\n" + 
        		"  </attributes>\n" + 
        		"</featureType>";
        
        MockHttpServletResponse response = 
            postAsServletResponse("/rest/workspaces/cite/datastores/default/featuretypes", xml, "text/xml");
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/cite/datastores/default/featuretypes/states" ) );
        
        FeatureTypeInfo ft = catalog.getFeatureTypeByName("cite", "states");
        assertNotNull(ft);
        FeatureType schema = ft.getFeatureType();
        assertEquals("states", schema.getName().getLocalPart());
        assertEquals(catalog.getNamespaceByPrefix("cite").getURI(), schema.getName().getNamespaceURI());
        assertEquals(3, schema.getDescriptors().size());
        assertNotNull(schema.getDescriptor("the_geom"));
        assertEquals(MultiPolygon.class, schema.getDescriptor("the_geom").getType().getBinding());
        assertNotNull(schema.getDescriptor("LAND_KM"));
        assertEquals(Double.class, schema.getDescriptor("LAND_KM").getType().getBinding());
    }

    public void testPostFillInMetadata() throws Exception {
        addPropertyDataStore(false);
        String xml = 
            "<featureType>"+
              "<name>pdsa</name>"+
            "</featureType>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/datastores/pds/featuretypes", xml, "text/xml");
        assertEquals( 201, response.getStatusCode() );

        FeatureTypeInfo ft = catalog.getFeatureTypeByName("gs", "pdsa");
        assertNotNull(ft);

        assertTrue(ft.getKeywords().contains(new Keyword("features")));
    }
}
