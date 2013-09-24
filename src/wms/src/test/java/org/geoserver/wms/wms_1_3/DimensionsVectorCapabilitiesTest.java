/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.w3c.dom.Document;

public class DimensionsVectorCapabilitiesTest extends WMSDimensionsTestSupport {
    
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);

        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='sf:TimeElevation'])", dom);
        assertXpathEvaluatesTo("0", "count(//wms:Layer/wms:Dimension)", dom);
    }
    
    public void testDefaultElevationUnits() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null, null, null);
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);

        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
    }
    
    public void testEmptyDataSet() throws Exception {
        for (DimensionPresentation p : DimensionPresentation.values()) {
            setupVectorDimension(V_TIME_ELEVATION_EMPTY.getLocalPart(), ResourceInfo.TIME, "time", p, null, null, null);
            checkEmptyTimeDimensionAndExtent();
        }

        // clear time metadata
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(V_TIME_ELEVATION_EMPTY.getLocalPart());
        info.getMetadata().remove(ResourceInfo.TIME);
        getCatalog().save(info);

        for (DimensionPresentation p : DimensionPresentation.values()) {
            setupVectorDimension(V_TIME_ELEVATION_EMPTY.getLocalPart(), ResourceInfo.ELEVATION, 
                    "elevation", p, null, UNITS, UNIT_SYMBOL);
            checkEmptyElevationDimensionAndExtent();
        }
    }

    void checkEmptyElevationDimensionAndExtent() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
//        print(dom);
        
        // check dimension info exists
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        // and that it is empty
        assertXpathEvaluatesTo("", "//wms:Layer/wms:Dimension", dom);
    }

    void checkEmptyTimeDimensionAndExtent() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);

        // check dimension info exists
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        // and that it is empty
        assertXpathEvaluatesTo("", "//wms:Layer/wms:Dimension", dom);
    }

    public void testElevationList() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0,1.0,2.0,3.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationContinuous() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.CONTINUOUS_INTERVAL, null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/3.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationDiscrerteNoResolution() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.DISCRETE_INTERVAL, null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/1.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationDiscrerteManualResolution() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.DISCRETE_INTERVAL, 2.0, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/2.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeList() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z,2011-05-02T00:00:00.000Z,2011-05-03T00:00:00.000Z,2011-05-04T00:00:00.000Z", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeContinuous() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.CONTINUOUS_INTERVAL, null, null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/P3D", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeResolution() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.DISCRETE_INTERVAL, new Double(1000 * 60 * 60 * 24), null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/P1D", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeElevation() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check both dimension has been declared
        assertXpathEvaluatesTo("2", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension[@name='elevation']/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension[@name='elevation']/@unitSymbol", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension[@name='time']/@units", dom);
        // check we have the extent for elevation        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension[@name='elevation'])", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension[@name='elevation']/@default", dom);
        assertXpathEvaluatesTo("0.0,1.0,2.0,3.0", "//wms:Layer/wms:Dimension[@name='elevation']", dom);
        // check we have the extent for time
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension[@name='time'])", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension[@name='time']/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z,2011-05-02T00:00:00.000Z,2011-05-03T00:00:00.000Z,2011-05-04T00:00:00.000Z", 
                "//wms:Layer/wms:Dimension[@name='time']", dom);
    }

}
