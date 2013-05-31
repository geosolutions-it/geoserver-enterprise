/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.logging.Level;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GeoSearchKMLTest extends RegionatingTestSupport {
    public void testOutput() throws Exception {
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart() + 
            "&styles=" + MockData.BASIC_POLYGONS.getLocalPart() + 
            "&height=1024&width=1024&bbox=-180,-90,0,90&srs=EPSG:4326" +  
            "&featureid=BasicPolygons.1107531493643";

        Document document = getAsDOM(path);
        assertEquals("kml", document.getDocumentElement().getTagName());;
    }

    /**
     * Test that requests regionated by data actually return stuff.
     */
    public void testDataRegionator() throws Exception{
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.DIVIDED_ROUTES.getPrefix() + ":" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&styles=" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&height=1024&width=1024&srs=EPSG:4326" +  
            "&format_options=regionateBy:external-sorting;regionateAttr:NUM_LANES";

        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        int westCount = document.getDocumentElement().getElementsByTagName("Placemark").getLength();

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");

        assertEquals(1, westCount);
    }

     /**
      * Test that requests regionated by geometry actually return stuff.
      */
     public void testGeometryRegionator() throws Exception{
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.DIVIDED_ROUTES.getPrefix() + ":" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&styles=" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&height=1024&width=1024&srs=EPSG:4326" +  
            "&format_options=regionateBy:geometry;regionateAttr:the_geom";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }
     
     /**
      * Test that requests regionated by random criteria actually return stuff.
      */
     public void testRandomRegionator() throws Exception{
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.DIVIDED_ROUTES.getPrefix() + ":" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&styles=" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&height=1024&width=1024&srs=EPSG:4326" +  
            "&format_options=regionateBy:random";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }

    /**
     * Test that when a bogus regionating strategy is requested things still work.
     * TODO: Evaluate whether an error message should be returned instead.
     */
    public void testBogusRegionator() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + MockData.DIVIDED_ROUTES.getPrefix() + ":" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&styles=" + MockData.DIVIDED_ROUTES.getLocalPart() + 
            "&height=1024&width=1024&srs=EPSG:4326" +  
            "&format_options=regionateBy:bogus";
         Document document = getAsDOM(path + "&bbox=0,-90,180,90", true);
         assertEquals("ServiceExceptionReport", document.getDocumentElement().getTagName());
    }


    /**
     * Test whether geometries that cross tiles get put into both of them.
     */
    public void testBigGeometries() throws Exception {
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + CENTERED_POLY.getPrefix() + ":" + CENTERED_POLY.getLocalPart() + 
            "&styles=" + 
            "&height=1024&width=1024&srs=EPSG:4326" +  
            "&format_options=regionateBy:external-sorting;regionateattr:foo";

        assertStatusCodeForGet(204, path +  "&bbox=-180,-90,0,90");
        
        Document document = getAsDOM(path + "&bbox=0,-90,180,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        
    }

    /**
     * Test whether specifying different regionating strategies changes the results.
     */
    public void testStrategyChangesStuff() throws Exception {
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + TILE_TESTS.getPrefix() + ":" + TILE_TESTS.getLocalPart() + 
            "&bbox=-180,-90,0,90&styles=" + 
            "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);
        getCatalog().save(fti);

        Document geo = getAsDOM(path + "&format_options=regionateBy:geometry;regionateattr:location");
        assertEquals("kml", geo.getDocumentElement().getTagName());

        NodeList geoPlacemarks = geo.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, geoPlacemarks.getLength());

        Document data = getAsDOM(path + "&format_options=regionateBy:external-sorting;regionateAttr:z");
        assertEquals("kml", data.getDocumentElement().getTagName());

        NodeList dataPlacemarks = data.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, dataPlacemarks.getLength());

        for (int i = 0; i < geoPlacemarks.getLength(); i++){
            String geoName = ((Element)geoPlacemarks.item(i)).getAttribute("id");
            String dataName = ((Element)dataPlacemarks.item(i)).getAttribute("id");

            assertTrue(geoName + " and " + dataName + " should not be the same!", 
                    !geoName.equals(dataName)
            );
        }
    }
    
    /**
     * Test whether specifying different regionating strategies changes the results.
     */
    public void testDuplicateAttribute() throws Exception {
        final String path = 
            "wms?request=getmap&service=wms&version=1.1.1" + 
            "&format=" + KMLMapOutputFormat.MIME_TYPE + 
            "&layers=" + TILE_TESTS.getPrefix() + ":" + TILE_TESTS.getLocalPart() + 
            "&bbox=-180,-90,0,90&styles=" + 
            "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);

        Document geo = getAsDOM(path + "&format_options=regionateBy:best_guess;regionateattr:the_geom");
        assertEquals("kml", geo.getDocumentElement().getTagName());
    }
}
