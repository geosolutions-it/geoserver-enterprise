/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.w3c.dom.Document;

public class GetFeatureInfoTest extends WMSTestSupport {
    
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);
    public static QName CUSTOM = new QName(MockData.CITE_URI, "custom", MockData.CITE_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        
        // setup buffer
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
        
        // force feature bounding in WFS
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        wfsInfo.setFeatureBounding(true);
        getGeoServer().save(wfsInfo);
        
        // add a wms store too, if possible
        if (RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            // setup the wms store, resource and layer
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMSStoreInfo wms = cb.buildWMSStore("demo");
            wms.setCapabilitiesURL(RemoteOWSTestSupport.WMS_SERVER_URL
                    + "service=WMS&request=GetCapabilities");
            getCatalog().save(wms);
            cb.setStore(wms);
            WMSLayerInfo states = cb.buildWMSLayer("topp:states");
            states.setName("rstates");
            getCatalog().add(states);
            LayerInfo layer = cb.buildLayer(states);
            getCatalog().add(layer);
        }
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("thickStroke", GetFeatureInfoTest.class.getResource("thickStroke.sld"));
        dataDirectory.addStyle("paramStroke", GetFeatureInfoTest.class.getResource("paramStroke.sld"));
        dataDirectory.addStyle("raster", GetFeatureInfoTest.class.getResource("raster.sld"));
        dataDirectory.addStyle("rasterScales", GetFeatureInfoTest.class.getResource("rasterScales.sld"));
        dataDirectory.addCoverage(TASMANIA_BM, GetFeatureInfoTest.class.getResource("tazbm.tiff"),
                "tiff", "raster");
        dataDirectory.addStyle("squares", GetFeatureInfoTest.class.getResource("squares.sld"));
        dataDirectory.addPropertiesType(SQUARES, GetFeatureInfoTest.class.getResource("squares.properties"),
                null);
        
        // this also adds the raster style
        dataDirectory.addCoverage(new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX), 
               MockData.class.getResource("raster-filter-test.zip"), null, "raster");
        
        // add a raster with a a custom projection
        dataDirectory.addCoverage(CUSTOM, GetFeatureInfoTest.class.getResource("custom.zip"), null, "raster");
    }
    
    /**
     * Tests GML output does not break when asking for an area that has no data with
     * GML feature bounding enabled
     * 
     * @throws Exception
     */
    public void testGMLNoData() throws Exception {
        String layer = getLayerId(MockData.PONDS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=application/vnd.ogc.gml&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=20&y=20";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", dom);
        assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);
    }
    
    /**
     * Tests GML outside of 
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        //System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    /**
     * Tests property selection 
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSelectPropertiesVector() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&service=wms" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&propertyName=NAME,FID";
        String result = getAsString(request);
        System.out.println(result);
        assertNotNull(result);
        int idxGeom = result.indexOf("the_geom");
        int idxFid = result.indexOf("FID");
        int idxName = result.indexOf("NAME");
        assertEquals(-1, idxGeom); // geometry filtered out
        assertTrue(idxFid > 0); 
        assertTrue(idxName > 0);
        assertTrue(idxName < idxFid); // properties got reordered as expected
    }
    
    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleHtml() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document dom = getAsDOM(request);
        
        // count lines that do contain a forest reference
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'Forests.')])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBuffer() throws Exception {
        // to setup the request and the buffer I rendered BASIC_POLYGONS using GeoServer, then played
        // against the image coordinates
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.1.1&bbox=-4.5,-2.,4.5,7&styles=&format=jpeg&info_format=text/html" +
                "&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300";
        Document dom = getAsDOM(base + "&x=85&y=230");
        // make sure the document is empty, as we chose an area with no features inside
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the extended buffer, make sure it's in
        dom = getAsDOM(base + "&x=85&y=230&buffer=40");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
        
        // this one would end up catching everything (3 features) if it wasn't that we say the max buffer at 50
        // in the WMS configuration
        dom = getAsDOM(base + "&x=85&y=230&buffer=300");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testAutoBuffer() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.1.1&bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html" +
                "&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300&x=114&y=229";
        Document dom = getAsDOM(base + "&styles=");
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make sure it's in
        dom = getAsDOM(base + "&styles=thickStroke");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo uses the env params
     * 
     * @throws Exception
     */
    public void testParameterizedStyle() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.1.1&bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html" +
                "&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300&x=114&y=229&styles=paramStroke";
        Document dom = getAsDOM(base);
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make sure it's in
        dom = getAsDOM(base + "&env=thickness:10");
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBufferScales() throws Exception {
        String layer = getLayerId(SQUARES);
        String base = "wms?version=1.1.1&format=png&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&styles=squares&bbox=0,0,10000,10000&feature_count=10";
        
        // first request, should provide no result, scale is 1:100
        int w = (int) (100.0 / 0.28 * 1000); // dpi compensation
        Document dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);
        
        // second request, should provide oe result, scale is 1:50
        w = (int) (200.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);

        // third request, should provide two result, scale is 1:10
        w = (int) (1000.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("2", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.2'])", dom);
        
    }
    
    /**
     * Tests a GetFeatureInfo again works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testTwoLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        // GEOS-2603 GetFeatureInfo returns html tables without css style if more than one layer is selected
        assertTrue(result.indexOf("<style type=\"text/css\">") > 0);
    }
    
    /**
     * Tests a GetFeatureInfo again works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSelectPropertiesTwoVectorLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&buffer=10&service=wms" 
                + "&feature_count=2&propertyName=(FID)(NAME)";
        String result = getAsString(request);
        assertNotNull(result);
        int idxGeom = result.indexOf("the_geom");
        int idxLakes= result.indexOf("Lakes");
        int idxFid = result.indexOf("FID");
        int idxName = result.indexOf("NAME");
        assertEquals(-1, idxGeom); // geometry filtered out
        assertTrue(idxFid > 0); 
        assertTrue(idxName > 0);
        assertTrue(idxLakes > 0);
        assertTrue(idxFid < idxLakes); // fid only for the first features
        assertTrue(idxName > idxLakes); // name only for the second features
    }
    
    /**
     * Tests a GetFeatureInfo again works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSelectPropertiesTwoVectorLayersOneList() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&buffer=10&service=wms" 
                + "&feature_count=2&propertyName=NAME";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        int idxGeom = result.indexOf("the_geom");
        int idxLakes= result.indexOf("Lakes");
        int idxName1 = result.indexOf("NAME");
        int idxName2 = result.indexOf("NAME", idxLakes);
        assertEquals(-1, idxGeom); // geometry filtered out

        assertTrue(idxName1 > 0);
        assertTrue(idxName2 > 0);
        assertTrue(idxLakes > 0);
        // name in both features
        assertTrue(idxName1 < idxLakes); 
        assertTrue(idxName2 > idxLakes);
    }
    
    /**
     * Tests that FEATURE_COUNT is respected globally, not just per layer
     * 
     * @throws Exception
     */
    public void testTwoLayersFeatureCount() throws Exception {
        // this request hits on two overlapping features, a lake and a forest
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml&" +
        		"BBOX=-0.002356%2C-0.004819%2C0.005631%2C0.004781&SERVICE=WMS&VERSION=1.1.0&X=267&Y=325" +
        		"&INFO_FORMAT=application/vnd.ogc.gml" +
        		"&QUERY_LAYERS=" + layer + "&Layers=" + layer + " &Styles=&WIDTH=426&HEIGHT=512" +
        	    "&format=image%2Fpng&srs=EPSG%3A4326";
        // no feature count, just one should be returned
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);
        
        // feature count set to 2, both features should be there
        dom = getAsDOM(request + "&FEATURE_COUNT=2");
        // print(dom);
        assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Lakes)", dom);
    }


    /**
     * Check GetFeatureInfo returns an error if the format is not known, instead
     * of returning the text format as in
     * http://jira.codehaus.org/browse/GEOS-1924
     * 
     * @throws Exception
     */
    public void testUknownFormat() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=unknown/format&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", doc);
        assertXpathEvaluatesTo("InvalidFormat", "/ServiceExceptionReport/ServiceException/@code", doc);
        assertXpathEvaluatesTo("info_format", "/ServiceExceptionReport/ServiceException/@locator", doc);
    }
    
    public void testCoverage() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
        		"&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
        		"&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testCoveragePropertySelection() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
                "&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326&propertyName=RED_BAND";
        Document dom = getAsDOM(request);
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testCoverageGML() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                        "&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
                        "&info_format=application/vnd.ogc.gml&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        //print(dom);
        
        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }
    
    public void testCoverageScales() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=rasterScales&bbox=146.5,-44.5,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        
        // this one should be blank
        Document dom = getAsDOM(request + "&width=300&height=300");
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
        
        // this one should draw the coverage
        dom = getAsDOM(request + "&width=600&height=600");
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testOutsideCoverage() throws Exception {
        // a request which is way large on the west side, lots of blank space
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=raster&bbox=0,-90,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&width=300&height=300&x=10&y=150&srs=EPSG:4326";
        
        // this one should be blank, but not be a service exception
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/html)", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
    }
    
    /**
     * Check we report back an exception when query_layer contains layers not part of LAYERS
     * @throws Exception
     */
    public void testUnkonwnQueryLayer() throws Exception {
        String layers1 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String layers2 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.BRIDGES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layers1 + "&query_layers=" + layers2 + "&width=20&height=20&x=10&y=10&info";
        
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/ServiceExceptionReport)", dom);
    }
    
    public void testLayerQualified() throws Exception {
        String layer = "Forests";
        String q = "?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String request = "cite/Ponds/wms" + q;
        Document dom = getAsDOM(request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        request = "cite/Forests/wms" + q;
        String result = getAsString(request);
        //System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testGroupWorkspaceQualified() throws Exception {
        // check the group works without workspace qualification
        String url = "wms?request=getmap&service=wms&version=1.1.1"
                + "&layers=nature&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002&info_format=text/plain" +
                		"&request=GetFeatureInfo&query_layers=nature&x=50&y=50&feature_count=2";
        String result = getAsString(url);
        assertTrue(result.indexOf("Blue Lake") > 0);
        assertTrue(result.indexOf("Green Forest") > 0);

        // check that it still works when workspace qualified
        result = getAsString("cite/" + url);
        assertTrue(result.indexOf("Blue Lake") > 0);
        assertTrue(result.indexOf("Green Forest") > 0);
        
        // but we have nothing if the workspace
        Document dom = getAsDOM("cdf/" + url);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testNonExactVersion() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.0.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        
        request = "wms?version=1.1.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
        "&info_format=text/plain&request=GetFeatureInfo&layers="
        + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        result = getAsString(request);
        
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testRasterFilterRed() throws Exception {
        String response = getAsString("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=" +
        		"&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetFeatureInfo&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150" +
                "&transparent=false&CQL_FILTER=location like 'red%25' + " +
                "&query_layers=sf:mosaic&x=10&y=10");
        
        assertTrue(response.indexOf("RED_BAND = 255.0") > 0);
        assertTrue(response.indexOf("GREEN_BAND = 0.0") > 0);
        assertTrue(response.indexOf("BLUE_BAND = 0.0") > 0);
    }
    
    public void testRasterFilterGreen() throws Exception {
        String response = getAsString("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=" +
                "&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetFeatureInfo&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150" +
                "&transparent=false&CQL_FILTER=location like 'green%25' + " +
                "&query_layers=sf:mosaic&x=10&y=10");
        
        assertTrue(response.indexOf("RED_BAND = 0.0") > 0);
        assertTrue(response.indexOf("GREEN_BAND = 255.0") > 0);
        assertTrue(response.indexOf("BLUE_BAND = 0.0") > 0);
    }
    
   public void testPropertySelectionWmsCascade() throws Exception {
       if (!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
           LOGGER.log(Level.WARNING, "Skipping testPropertySelectionWmsCascade");
           return;
       }
       
       String result = getAsString("wms?REQUEST=GetFeatureInfo" +
       		"&BBOX=-132.835937%2C21.132813%2C-64.867187%2C55.117188" +
       		"&SERVICE=WMS&INFO_FORMAT=text/plain" +
       		"&QUERY_LAYERS=rstates&FEATURE_COUNT=50&Layers=rstates&WIDTH=300&HEIGHT=150" +
       		"&format=image%2Fpng&styles=&srs=EPSG%3A4326&version=1.1.1&x=149&y=70&propertyName=STATE_ABBR,STATE_NAME");
       
       // System.out.println(result);
       
       int idxGeom = result.indexOf("the_geom");
       int idxName = result.indexOf("STATE_NAME");
       int idxFips = result.indexOf("STATE_FIPS");
       int idxAbbr = result.indexOf("STATE_ABBR");
       assertEquals(-1, idxGeom);
       assertEquals(-1, idxFips);
       assertTrue(idxAbbr > 0);
       assertTrue(idxName > 0);
       assertTrue(idxAbbr < idxName);
   }
    
   
   public void testRasterKeepNative() throws Exception {
       // force it to "keep native"
       CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(CUSTOM));
       ci.setProjectionPolicy(ProjectionPolicy.NONE);
       getCatalog().save(ci);
       
       // make a first reprojected request on a pixel that's black (0)
       String result = getAsString("wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml" +
       		"&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS" +
       		"&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom" +
       		"&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.1.1&x=177&y=225");
       assertTrue(result.contains("0.0"));
       
       // and now one with actual data, 2
       result = getAsString("wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml" +
               "&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS" +
               "&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom" +
               "&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.1.1&x=135&y=223");
       assertTrue(result.contains("2.0"));
   }

   public void testGMLWithPostFilter() throws Exception {
       //we need to create a situation where a post filter is setup, simple way is to change the 
       // style so that its filter is an or with more than 20 children
       Catalog cat = getCatalog();
       LayerInfo l = cat.getLayerByName(getLayerId(MockData.NAMED_PLACES));

       StyleInfo style = l.getDefaultStyle(); 
       Style s = style.getStyle();

       FeatureTypeStyle fts = s.featureTypeStyles().get(0);
       FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
       StyleFactory sf = CommonFactoryFinder.getStyleFactory();
       for (int i = 0; i < 21; i++) {
           Filter f = ff.equals(ff.literal(1), ff.literal(1));
           Rule r = sf.createRule();
           r.setFilter(f);
           r.symbolizers().add(sf.createPolygonSymbolizer());
           fts.rules().add(r);
       }

       cat.getResourcePool().writeStyle(style, s);
       cat.save(style);

       String layer = getLayerId(MockData.NAMED_PLACES);
       
       String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                       "&layers=" + layer + "&styles=&bbox=0.000004,-0.00285,0.005596,0.00415&width=409&height=512" + 
                       "&info_format=application/vnd.ogc.gml&query_layers=" + layer + "&x=194&y=229&srs=EPSG:4326";
       Document dom = getAsDOM(request);
       assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
   }
}
