/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.GetFeatureInfoKvpReader;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

/**
 * A GetFeatureInfo 1.3.0 integration test suite covering both spec mandates and geoserver specific
 * features.
 */
public class GetFeatureInfoIntegrationTest extends WMSTestSupport {

    public static String WCS_PREFIX = "wcs";

    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);

    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoIntegrationTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put(WCS_PREFIX, WCS_URI);

        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);

        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs10Coverages();
        dataDirectory.addStyle("thickStroke", getClass()
                .getResource("../wms_1_1_1/thickStroke.sld"));
        dataDirectory.addStyle("raster", getClass().getResource("../wms_1_1_1/raster.sld"));
        dataDirectory.addStyle("rasterScales",
                getClass().getResource("../wms_1_1_1/rasterScales.sld"));
        dataDirectory.addCoverage(TASMANIA_BM, getClass().getResource("../wms_1_1_1/tazbm.tiff"),
                "tiff", "raster");
        dataDirectory.addStyle("forestsManyRules", getClass().getResource("../wms_1_1_1/ForestsManyRules.sld"));
        dataDirectory.addStyle("squares", getClass().getResource("../wms_1_1_1/squares.sld"));
        dataDirectory.addPropertiesType(SQUARES,
                getClass().getResource("../wms_1_1_1/squares.properties"), null);
    }

    /**
     * As per section 7.4.1, a client shall not issue a GetFeatureInfo request for non queryable
     * layers; yet that section is not too clear with regard to whether an exception should be
     * thrown. I read it like an exception with OperationNotSupported code should be thrown. The
     * full text is:
     * <p>
     * <i> GetFeatureInfo is an optional operation. It is only supported for those Layers for which
     * the attribute queryable="1" (true) has been defined or inherited. A client shall not issue a
     * GetFeatureInfo request for other layers. A WMS shall respond with a properly formatted
     * service exception (XML) response (code = OperationNotSupported) if it receives a
     * GetFeatureInfo request but does not support it. </i>
     * </p>
     */
    public void testQueryNonQueryableLayer() throws Exception {
        // HACK: fake the WMS facade to inform the layer is non queryable. Looks like we would need
        // a LayerInfo.isQueryable() property
        final WMS wms = (WMS) applicationContext.getBean("wms");
        GetFeatureInfoKvpReader reader = (GetFeatureInfoKvpReader) applicationContext
                .getBean("getFeatureInfoKvpReader");
        try {
            WMS fakeWMS = new WMS(wms.getGeoServer()) {
                @Override
                public boolean isQueryable(LayerInfo layer) {
                    if ("Forests".equals(layer.getName())) {
                        return false;
                    }
                    return super.isQueryable(layer);
                }
            };

            reader.setWMS(fakeWMS);

            String layer = getLayerId(MockData.FORESTS);
            String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                    + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
            Document doc = dom(get(request), true);

            assertXpathEvaluatesTo("LayerNotQueryable",
                    "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
        } finally {
            // restore the original wms
            reader.setWMS(wms);
        }
    }

    /**
     * As for section 7.4.3.7, a missing or incorrectly specified pair of I,J parameters shall issue
     * a service exception with {@code InvalidPoint} code.
     * 
     * @throws Exception
     */
    public void testInvalidPoint() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        // missing I,J parameters
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20";
        Document doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("InvalidPoint",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);

        // invalid I,J parameters
        request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=A&j=";
        doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("InvalidPoint",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
    }

    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the expected polygon
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testCustomTemplateManyRules() throws Exception {
        // setup custom template
        File root = getTestData().getDataDirectoryRoot();
        File target = new File(root, "workspaces/" + MockData.FORESTS.getPrefix() + "/content.ftl");
        File source = new File("./src/test/resources/org/geoserver/wms/content.ftl");
        try {
            assertTrue(source.exists());
            FileUtils.copyFile(source, target);

            // request with default style, just one rule
            String layer = getLayerId(MockData.FORESTS);
            String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                    + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
            Document dom = getAsDOM(request);
            // print(dom);

            assertXpathExists("/html/body/ul/li/b[text() = 'Type: Forests']", dom);

            // request with a style having 21 rules, used to fail, see GEOS-5534
            request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=forestsManyRules&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                    + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
            dom = getAsDOM(request);
            // print(dom);

            assertXpathExists("/html/body/ul/li/b[text() = 'Type: Forests']", dom);
        } finally {
            FileUtils.deleteQuietly(target);
        }
    }

    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleHtml() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
        Document dom = getAsDOM(request);
        // print(dom);
        // count lines that do contain a forest reference
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'Forests.')])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     * 
     * @throws Exception
     */
    public void testBuffer() throws Exception {
        // to setup the request and the buffer I rendered BASIC_POLYGONS using GeoServer, then
        // played
        // against the image coordinates
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.3.0&bbox=-4.5,-2.,4.5,7&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300";
        Document dom = getAsDOM(base + "&i=85&j=230");
        // make sure the document is empty, as we chose an area with no features inside
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the extended buffer, make sure it's in
        dom = getAsDOM(base + "&i=85&j=230&buffer=40");
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);

        // this one would end up catching everything (3 features) if it wasn't that we say the max
        // buffer at 50
        // in the WMS configuration
        dom = getAsDOM(base + "&i=85&j=230&buffer=300");
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }

    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     * 
     * @throws Exception
     */
    public void testAutoBuffer() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.3.0&bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300&i=114&j=229";
        Document dom = getAsDOM(base + "&styles=");
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make
        // sure it's in
        dom = getAsDOM(base + "&styles=thickStroke");
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1",
                "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }

    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     * 
     * @throws Exception
     */
    public void testBufferScales() throws Exception {
        String layer = getLayerId(SQUARES);
        String base = "wms?version=1.3.0&&format=png&info_format=text/html&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&styles=squares&bbox=0,0,10000,10000&feature_count=10";

        // first request, should provide no result, scale is 1:100
        int w = (int) (100.0 / 0.28 * 1000); // dpi compensation
        Document dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&i=20&j=" + (w - 20));
        // print(dom);
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // second request, should provide oe result, scale is 1:50
        w = (int) (200.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&i=20&j=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);

        // third request, should provide two result, scale is 1:10
        w = (int) (1000.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&i=20&j=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("2", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.2'])", dom);

    }

    /**
     * Tests a GetFeatureInfo againworks, and that the result contains the expected polygon
     * 
     * @throws Exception
     */
    public void testTwoLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10&info";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        // GEOS-2603 GetFeatureInfo returns html tables without css style if more than one layer is
        // selected
        assertTrue(result.indexOf("<style type=\"text/css\">") > 0);

    }

    /**
     * Check GetFeatureInfo returns an error if the format is not known, instead of returning the
     * text format as in http://jira.codehaus.org/browse/GEOS-1924
     * 
     * @throws Exception
     */
    public void testUknownFormat() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=unknown/format&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
        Document doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", doc);
        assertXpathEvaluatesTo("InvalidFormat",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
        assertXpathEvaluatesTo("info_format",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@locator", doc);
    }

    public void testCoverage() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo" + "&layers="
                + layer + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                + "&info_format=text/html&query_layers=" + layer + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }

    public void testCoverageGML() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo" + "&layers="
                + layer + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                + "&info_format=application/vnd.ogc.gml&query_layers=" + layer
                + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);

        assertXpathEvaluatesTo("26.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    public void testCoverageScales() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo" + "&layers="
                + layer + "&styles=rasterScales&bbox=-44.5,146.5,-43,148"
                + "&info_format=text/html&query_layers=" + layer + "&i=300&j=300&srs=EPSG:4326";

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
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo" + "&layers="
                + layer + "&styles=raster&bbox=0,-90,148,-43"
                + "&info_format=text/html&query_layers=" + layer
                + "&width=300&height=300&i=10&j=150&srs=EPSG:4326";

        // this one should be blank, but not be a service exception
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/html)", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
    }

    /**
     * Check we report back an exception when query_layer contains layers not part of LAYERS
     * 
     * @throws Exception
     */
    public void testUnknownQueryLayer() throws Exception {
        String layers1 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String layers2 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.BRIDGES);
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layers1 + "&query_layers=" + layers2 + "&width=20&height=20&i=10&j=10&info";

        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/ogc:ServiceExceptionReport)", dom);
    }

    public void testDeriveLayersFromSLD() throws Exception {
        String layers = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        
        String sld =
            "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" " +
            "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "+
            " <NamedLayer> "+
            "  <se:Name>"+getLayerId(MockData.FORESTS)+"</se:Name> "+
            " </NamedLayer> "+
            " <NamedLayer> "+
            "  <se:Name>"+getLayerId(MockData.LAKES)+"</se:Name> "+
            " </NamedLayer> "+
            "</StyledLayerDescriptor>";

        // sld present & query_layers null/empty
        String request1 = "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                + "sld_body=" + sld.replaceAll("=", "%3D") + "&width=20&height=20&x=10&y=10&info";

        // sld present & query_layers equals layers derived by sld
        String request2 = "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                + "sld_body=" + sld.replaceAll("=", "%3D") + "&query_layers=" + layers + "&width=20&height=20&x=10&y=10&info";

        // normal request
        String request3 = "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                + "layers=" + layers + "&query_layers=" + layers + "&width=20&height=20&x=10&y=10&info";

        // sld not present & query_layers null
        String invalidRequest1 = "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                + "layers=" + layers + "&width=20&height=20&x=10&y=10&info";

        // sld present & query_layers contains unknown layer
        String invalidRequest2 = "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                + "sld_body=" + sld.replaceAll("=", "%3D") + "&query_layers=" + getLayerId(MockData.TASMANIA_BM) + "&width=20&height=20&x=10&y=10&info";

        String result1 = getAsString(request1);
        String result2 = getAsString(request2);
        String result3 = getAsString(request3);

        assertEquals(result1, result2);
        assertEquals(result1, result3);

        Document invalidResult1 = getAsDOM(invalidRequest1);
        Document invalidResult2 = getAsDOM(invalidRequest2);
        print(invalidResult1);
        assertXpathEvaluatesTo("1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", invalidResult1);
        assertXpathEvaluatesTo("1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", invalidResult2);
    }

    public void testLayerQualified() throws Exception {
        String layer = "Forests";
        String q = "?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&i=10&j=10";
        String request = "cite/Ponds/wms" + q;
        Document dom = getAsDOM(request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

        request = "cite/Forests/wms" + q;
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    public void testXY() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testXYGeo() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String url = "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
            + layer + "&query_layers=" + layer + "&width=292&height=512&x=147&y=360&srs=epsg:4326";
        
        String request = url + "&VERSION=1.1.1&BBOX=-3.992187,-4.5,3.992188,9.5";
        String result = getAsString(request);
        assertTrue(result.indexOf("the_geom =") > 0);
        
        request = url + "&VERSION=1.3.0&BBOX=-4.5,-3.992187,9.5,3.992188";
        result = getAsString(request);
        assertTrue(result.indexOf("the_geom =") > 0);
    }
    
    public void testXYProj() throws Exception {
        String layer = getLayerId(MockData.POLYGONS);
        String url = "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
            + layer + "&query_layers=" + layer + "&WIDTH=512&HEIGHT=511&X=136&Y=380&srs=epsg:32615";
        
        String request = url + "&VERSION=1.1.1&BBOX=499699.999705,499502.050472,501800.000326,501597.949528";
        String result = getAsString(request);
        assertTrue(result.indexOf("polygonProperty =") > 0);
        
        request = url + "&VERSION=1.3.0&BBOX=499699.999705,499502.050472,501800.000326,501597.949528";
        result = getAsString(request);
        assertTrue(result.indexOf("polygonProperty =") > 0);
    }
    
    public void testXYCoverage() throws Exception {
        String layer = getLayerId(MockData.USA_WORLDIMG);
        String url = "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
            + layer + "&query_layers=" + layer + "&WIDTH=512&HEIGHT=408&X=75&Y=132&srs=epsg:4326";
        
        String request = url + "&VERSION=1.1.1&BBOX=-180,-143.4375,180,143.4375";
        String result = getAsString(request);
        Matcher m = Pattern.compile(
            ".*RED_BAND = (\\d+\\.\\d+).*GREEN_BAND = (\\d+\\.\\d+).*BLUE_BAND = (\\d+\\.\\d+).*", 
            Pattern.DOTALL).matcher(result);
        assertTrue(m.matches());
        double red = Double.parseDouble(m.group(1));
        double green = Double.parseDouble(m.group(2));
        double blue = Double.parseDouble(m.group(3));
        
        request = url + "&VERSION=1.3.0&BBOX=-143.4375,-180,143.4375,180";
        result = getAsString(request);
        
        m = Pattern.compile(
            ".*RED_BAND = (\\d+\\.\\d+).*GREEN_BAND = (\\d+\\.\\d+).*BLUE_BAND = (\\d+\\.\\d+).*", 
            Pattern.DOTALL).matcher(result);
        assertTrue(m.matches());
        assertEquals(red, Double.parseDouble(m.group(1)));
        assertEquals(green, Double.parseDouble(m.group(2)));
        assertEquals(blue, Double.parseDouble(m.group(3)));
    }
}
