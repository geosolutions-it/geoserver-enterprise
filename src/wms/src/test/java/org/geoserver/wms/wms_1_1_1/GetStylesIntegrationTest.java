/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.io.InputStream;

import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;

public class GetStylesIntegrationTest extends WMSTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetStylesIntegrationTest());
    }

    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        Catalog catalog = getCatalog();
        
        String lakes = MockData.LAKES.getLocalPart();
        String forests = MockData.FORESTS.getLocalPart();
        String bridges = MockData.BRIDGES.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("lakesGroup");
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        lg.getLayers().add(catalog.getLayerByName(forests));
        lg.getStyles().add(catalog.getStyleByName(forests));
        lg.getLayers().add(catalog.getLayerByName(bridges));
        lg.getStyles().add(catalog.getStyleByName(bridges));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);

        // makes the lakes layer a multi-style one
        LayerInfo lakesLayer = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakesLayer.getStyles().add(catalog.getStyleByName(MockData.FORESTS.getLocalPart()));
        catalog.save(lakesLayer);
    }
    
    public void testSimple() throws Exception {
        InputStream stream = get("wms?service=WMS&version=1.1.1&&request=GetStyles&layers="
                + getLayerId(MockData.BASIC_POLYGONS) + "&sldver=1.0.0");
        
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
        parser.setInput(stream);
        
        StyledLayerDescriptor sld = parser.parseSLD();
        assertEquals(1, sld.getStyledLayers().length);
        
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
        assertEquals(1, layer.styles().size());
        
        Style style = layer.styles().get(0);
        assertTrue(style.isDefault());
        assertEquals("BasicPolygons", style.getName());
    }
    
    public void testGroup() throws Exception {
        InputStream stream = get("wms?service=WMS&version=1.1.1&request=GetStyles&layers=lakesGroup&sldver=1.0.0");
        
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
        parser.setInput(stream);
        
        StyledLayerDescriptor sld = parser.parseSLD();
        assertEquals(1, sld.getStyledLayers().length);
        
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        assertEquals("lakesGroup", layer.getName());
        
        // groups have no style
        assertEquals(0, layer.styles().size());
    }
    
    public void testMultiStyle() throws Exception {
        InputStream stream = get("wms?service=WMS&version=1.1.1&request=GetStyles&layers="
                + getLayerId(MockData.LAKES) + "&sldver=1.0.0");
        
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null));
        parser.setInput(stream);
        
        StyledLayerDescriptor sld = parser.parseSLD();
        assertEquals(1, sld.getStyledLayers().length);
        
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        assertEquals(getLayerId(MockData.LAKES), layer.getName());
        assertEquals(2, layer.styles().size());
        
        Style style = layer.styles().get(0);
        assertTrue(style.isDefault());
        assertEquals("Lakes", style.getName());
        
        style = layer.styles().get(1);
        assertFalse(style.isDefault());
        assertEquals("Forests", style.getName());
    }
}
