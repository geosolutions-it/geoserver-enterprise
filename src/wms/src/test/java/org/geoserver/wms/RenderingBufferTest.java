/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.awt.image.BufferedImage;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Tests that the admin specified per layer buffer parameter is taken into account
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class RenderingBufferTest extends WMSTestSupport {

    static final QName LINE_WIDTH_LAYER = new QName(MockData.CITE_URI, "LineWidth", MockData.CITE_PREFIX);

    static final String LINE_WIDTH_STYLE = "linewidth";

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        dataDirectory.addStyle(LINE_WIDTH_STYLE, getClass().getResource("linewidth.sld"));
        dataDirectory.addPropertiesType(LINE_WIDTH_LAYER, getClass().getResource(
                "LineWidth.properties"), Collections.singletonMap(MockData.KEY_STYLE,
                LINE_WIDTH_STYLE));
    }

    public void testGetMapNoBuffer() throws Exception {
        String request = "cite/wms?request=getmap&service=wms" + "&layers="
                + getLayerId(LINE_WIDTH_LAYER) + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
        
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertEquals(0, countNonBlankPixels("testGetMap", image, BG_COLOR));
    }
    
    public void testGetFeatureInfoNoBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request = "cite/wms?request=getfeatureinfo&service=wms" + "&layers="
                + layerName + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers=" 
                + layerName + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);
    }

    
    public void testGetMapExplicitBuffer() throws Exception {
        String request = "cite/wms?request=getmap&service=wms" + "&layers="
                + getLayerId(LINE_WIDTH_LAYER) + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5&buffer=30";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
        
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertTrue(countNonBlankPixels("testGetMap", image, BG_COLOR) > 0);
    }
    
    public void testGetFeatureInfoExplicitBuffer() throws Exception {
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request = "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms" + "&layers="
                + layerName + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers=" 
                + layerName + "&info_format=application/vnd.ogc.gml&buffer=30";
        Document dom = getAsDOM(request);
        //print(dom);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }
    
    public void testGetMapConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);
        
        String request = "cite/wms?request=getmap&service=wms" + "&layers="
                + getLayerId(LINE_WIDTH_LAYER) + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5";
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("image/png", response.getContentType());
        
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        showImage("testGetMap", image);
        assertTrue(countNonBlankPixels("testGetMap", image, BG_COLOR) > 0);
    }
    
    public void testGetFeatureInfoConfiguredBuffer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINE_WIDTH_LAYER));
        layer.getMetadata().put(LayerInfo.BUFFER, 30);
        catalog.save(layer);
        
        final String layerName = getLayerId(LINE_WIDTH_LAYER);
        String request = "cite/wms?version=1.1.1&request=getfeatureinfo&service=wms" + "&layers="
                + layerName + "&styles=" + LINE_WIDTH_STYLE
                + "&width=50&height=50&format=image/png" + "&srs=epsg:4326&bbox=-6,0,-1,5&x=49&y=49&query_layers=" 
                + layerName + "&info_format=application/vnd.ogc.gml";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }

}
