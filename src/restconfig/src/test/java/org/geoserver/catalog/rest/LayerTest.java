/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LayerTest extends CatalogRESTTestSupport {

    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layers/cite:Buildings.xml");
        assertEquals( "layer", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("Buildings", "/layer/name", dom );
        // check the layer name is actually the first child (GEOS-3336 risked modifying
        // the order)
        assertXpathEvaluatesTo("Buildings", "/layer/*[1]", dom );
        assertXpathEvaluatesTo("http://localhost/geoserver/rest/styles/Buildings.xml",
                "/layer/defaultStyle/atom:link/attribute::href", dom);
    }
    
    public void testGetAsHTML() throws Exception {
        getAsDOM("/rest/layers/cite:Buildings.html" );
    }
    
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layers.xml");
        assertXpathEvaluatesTo(catalog.getLayers().size()+"", "count(//layer)", dom );
    }
    
    public void testGetAllAsHTML() throws Exception {
        getAsDOM( "/rest/layers.html");
    }
    
    public void testPut() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:Buildings" );
        assertEquals( "Buildings", l.getDefaultStyle().getName() );
        String xml = 
            "<layer>" +
              "<defaultStyle>Forests</defaultStyle>" + 
              "<styles>" + 
                "<style>Ponds</style>" +
              "</styles>" + 
            "</layer>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/layers/cite:Buildings", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        l = catalog.getLayerByName("cite:Buildings");
        assertEquals( "Forests", l.getDefaultStyle().getName() );
    }
    
    public void testDelete() throws Exception {
        assertNotNull(catalog.getLayerByName( "cite:Buildings" ));
        
        assertEquals(200, deleteAsServletResponse("/layers/cite:Buildings").getStatusCode());
    }
    
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getLayerByName( "cite:Buildings" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Buildings" ));
        
        assertEquals(200, deleteAsServletResponse("/rest/layers/cite:Buildings").getStatusCode());
        
        assertNull(catalog.getLayerByName( "cite:Buildings" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Buildings" ));
        
        assertNotNull(catalog.getLayerByName( "cite:Bridges" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Bridges" ));
        
        assertEquals(200, deleteAsServletResponse("/rest/layers/cite:Bridges?recurse=true").getStatusCode());
    
        assertNull(catalog.getLayerByName( "cite:Bridges" ));
        assertNull(catalog.getFeatureTypeByName( "cite", "Bridges" ));
    }

    public void testPutWorkspaceStyle() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));
        assertNull(cat.getStyleByName("cite", "foo"));

        String xml = 
            "<style>" +
              "<name>foo</name>" +
              "<filename>foo.sld</filename>" + 
            "</style>";

        MockHttpServletResponse response =
            postAsServletResponse("/rest/workspaces/cite/styles", xml);
        assertEquals(201, response.getStatusCode());
        assertNotNull(cat.getStyleByName("cite", "foo"));

        xml = 
            "<layer>" + 
                "<defaultStyle>" + 
                    "<name>foo</name>" +
                    "<workspace>cite</workspace>" +
                "</defaultStyle>" +
                "<enabled>true</enabled>" + 
            "</layer>";
        response =
            putAsServletResponse("/rest/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatusCode());

        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultStyle());
        assertEquals("foo", l.getDefaultStyle().getName());
        assertNotNull(l.getDefaultStyle().getWorkspace());

        Document dom = getAsDOM("/rest/layers/cite:Buildings.xml");
        assertXpathExists("/layer/defaultStyle/name[text() = 'foo']", dom);
        assertXpathEvaluatesTo("http://localhost/geoserver/rest/workspaces/cite/styles/foo.xml", 
            "//defaultStyle/atom:link/@href", dom );
    }
}
