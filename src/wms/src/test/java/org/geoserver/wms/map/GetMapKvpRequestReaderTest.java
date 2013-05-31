/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.Test;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.kvp.PaletteManager;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Style;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;

@SuppressWarnings("unchecked")
public class GetMapKvpRequestReaderTest extends KvpRequestReaderTestSupport {
    GetMapKvpRequestReader reader;

    Dispatcher dispatcher;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetMapKvpRequestReaderTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        CatalogFactory cf = getCatalog().getFactory();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        LayerGroupInfo gi = cf.createLayerGroup();
        gi.setName("testGroup");
        gi.getLayers().add(getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()));
        gi.getStyles().add(getCatalog().getStyleByName("polygon"));
        cb.calculateLayerGroupBounds(gi);
        getCatalog().add(gi);
    }
    
    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        // reset the legacy flag so that other tests are not getting affected by it
        GeoServerLoader.setLegacy(false);
    }

    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
        WMS wms = new WMS(getGeoServer());
        reader = new GetMapKvpRequestReader(wms);
    }

    public void testCreateRequest() throws Exception {
        GetMapRequest request = (GetMapRequest) reader.createRequest();
        assertNotNull(request);
    }

    public void testReadMandatory() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("styles", MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        String layer = MockData.BASIC_POLYGONS.getLocalPart();
        assertEquals(1, request.getLayers().size());
        assertTrue(request.getLayers().get(0).getName().endsWith(layer));

        assertEquals(1, request.getStyles().size());
        Style expected = getCatalog().getStyleByName(layer).getStyle();
        Style style = request.getStyles().get(0);
        assertEquals(expected, style);

        assertEquals("image/jpeg", request.getFormat());
        assertEquals(600, request.getHeight());
        assertEquals(800, request.getWidth());

        assertNotNull(request.getBbox());
        assertEquals(-10d, request.getBbox().getMinX(), 0);
        assertEquals(-10d, request.getBbox().getMinY(), 0);
        assertEquals(10d, request.getBbox().getMaxX(), 0);
        assertEquals(10d, request.getBbox().getMaxY(), 0);

        assertEquals("epsg:3003", request.getSRS());
    }

    public void testReadOptional() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("bgcolor", "000000");
        kvp.put("transparent", "true");
        kvp.put("tiled", "true");
        kvp.put("tilesorigin", "1.2,3.4");
        kvp.put("buffer", "1");
        kvp.put("palette", "SAFE");
        kvp.put("time", "2006-02-27T22:08:12Z");
        kvp.put("elevation", "4");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertEquals(Color.BLACK, request.getBgColor());
        assertTrue(request.isTransparent());
        assertTrue(request.isTiled());

        assertEquals(new Point2D.Double(1.2, 3.4), request.getTilesOrigin());
        assertEquals(1, request.getBuffer());

        assertEquals(PaletteManager.safePalette, request.getPalette());
        assertEquals(Arrays.asList(4.0), request.getElevation());

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(2006, 1, 27, 22, 8, 12);
        List<Object> times = request.getTime();
        assertEquals(1, request.getTime().size());
        assertEquals(cal.getTime().toString(), times.get(0).toString());
    }

    public void testDefaultStyle() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                        + "," + MockData.BUILDINGS.getPrefix() + ":"
                        + MockData.BUILDINGS.getLocalPart());
        raw.put("styles", ",");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
        assertEquals(2, request.getStyles().size());
        LayerInfo basicPolygons = getCatalog().getLayerByName(
                MockData.BASIC_POLYGONS.getLocalPart());
        LayerInfo buildings = getCatalog().getLayerByName(MockData.BUILDINGS.getLocalPart());
        assertEquals(basicPolygons.getDefaultStyle().getStyle(), request.getStyles().get(0));
        assertEquals(buildings.getDefaultStyle().getStyle(), request.getStyles().get(1));
    }

    public void testFilter() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        kvp.put("filter", "<Filter><FeatureId id=\"foo\"/></Filter>");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getFilter());
        assertEquals(1, request.getFilter().size());

        Id fid = (Id) request.getFilter().get(0);
        assertEquals(1, fid.getIDs().size());

        assertEquals("foo", fid.getIDs().iterator().next());
    }

    public void testCQLFilter() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("cql_filter", "foo = bar");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getCQLFilter());
        assertEquals(1, request.getCQLFilter().size());

        PropertyIsEqualTo filter = (PropertyIsEqualTo) request.getCQLFilter().get(0);
    }

    public void testFeatureId() throws Exception {
        HashMap kvp = new HashMap();
        kvp.put("featureid", "foo");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getFeatureId());
        assertEquals(1, request.getFeatureId().size());

        assertEquals("foo", request.getFeatureId().get(0));
    }

    public void testSldNoDefault() throws Exception {
        // no style name, no default, we should fall back on the server default
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        // the kvp should be already in decoded form
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        reader.setLaxStyleMatchAllowed(false);
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));

        assertNotNull(request.getSld());
        assertEquals(url, request.getSld());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("BasicPolygons", style.getName());
    }

    public void testSldDefault() throws Exception {
        // no style name, but the sld has a default for that layer
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryDefault.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(url, request.getSld());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

    public void testSldNamed() throws Exception {
        // style name matching one in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(url, request.getSld());
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
    }

    public void testSldFailLookup() throws Exception {
        // nothing matches the required style name
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsLibraryNoDefault.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "ThisStyleDoesNotExists");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            reader.setLaxStyleMatchAllowed(false);
            request = (GetMapRequest) reader.read(request, parseKvp(kvp), caseInsensitiveKvp(kvp));
            fail("The style looked up, 'ThisStyleDoesNotExists', should not have been found");
        } catch (ServiceException e) {
            // System.out.println(e);
        }
    }

    public void testSldFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(url, request.getSld());
        // check the style
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(ff.equals(ff.property("ID"), ff.literal("xyz")),
                layer.getLayerFeatureConstraints()[0].getFilter());
    }

    public void testSldLibraryFeatureTypeConstraints() throws Exception {
        // no styles, no layer, the full definition is in the sld
        HashMap kvp = new HashMap();
        URL url = GetMapKvpRequestReader.class.getResource("BasicPolygonsFeatureTypeConstaint.sld");
        kvp.put("sld", URLDecoder.decode(url.toExternalForm(), "UTF-8"));
        kvp.put("layers",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        kvp.put("styles", "TheLibraryModeStyle");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertNotNull(request.getSld());
        assertEquals(url, request.getSld());
        // check the style
        final Style style = (Style) request.getStyles().get(0);
        assertNotNull(style);
        assertEquals("TheLibraryModeStyle", style.getName());
        // check the layer
        assertEquals(1, request.getLayers().size());
        MapLayerInfo layer = request.getLayers().get(0);
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), layer.getName());
        // check the filter imposed in the feature type constraint
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        assertEquals(1, layer.getLayerFeatureConstraints().length);
        assertEquals(ff.equals(ff.property("ID"), ff.literal("xyz")),
                layer.getLayerFeatureConstraints()[0].getFilter());
    }

    /**
     * One of the cite tests ensures that WMTVER is recognized as VERSION and the server does not
     * complain
     * 
     * @throws Exception
     */
    public void testWmtVer() throws Exception {
        dispatcher.setCiteCompliant(true);
        String request = "wms?SERVICE=WMS&&WiDtH=200&FoRmAt=image/png&LaYeRs=cite:Lakes&StYlEs=&BbOx=0,-0.0020,0.0040,0&ReQuEsT=GetMap&HeIgHt=100&SrS=EPSG:4326&WmTvEr=1.1.1";
        assertEquals("image/png", getAsServletResponse(request).getContentType());
    }

    public void testRemoteWFS() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("styles", MockData.BASIC_POLYGONS.getLocalPart());
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        assertEquals("WFS", request.getRemoteOwsType()); // TODO: handle case?
        assertEquals(new URL(RemoteOWSTestSupport.WFS_SERVER_URL), request.getRemoteOwsURL());
        assertEquals(1, request.getLayers().size());
        assertEquals(LayerInfo.Type.REMOTE.getCode().intValue(), request.getLayers().get(0)
                .getType());
        assertEquals("topp:states", request.getLayers().get(0).getName());
    }

    public void testRemoteWFSNoStyle() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", RemoteOWSTestSupport.WFS_SERVER_URL);

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the missing style");
        } catch (ServiceException e) {
            assertEquals("NoDefaultStyle", e.getCode());
        }
    }

    public void testRemoteWFSInvalidURL() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;

        HashMap raw = new HashMap();
        raw.put("layers", "topp:states");
        raw.put("format", "image/png");
        raw.put("srs", "epsg:4326");
        raw.put("bbox", "-100,20,-60,50");
        raw.put("height", "300");
        raw.put("width", "300");
        raw.put("remote_ows_type", "WFS");
        raw.put("remote_ows_url", "http://phantom.openplans.org:8080/crapserver/wfs?");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        try {
            request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));
            fail("This should have thrown an exception because of the non existent layer");
        } catch (ServiceException e) {
            e.printStackTrace();
            assertEquals("RemoteOWSFailure", e.getCode());
        }
    }

    public void testGroupInSLD() throws Exception {
        // see GEOS-1818
        final HashMap kvp = new HashMap();
        kvp.put("srs", "epsg:4326");
        kvp.put("bbox",
                "124.38035938267053,-58.45445933799711,169.29632161732948,-24.767487662002893");
        kvp.put("width", "640");
        kvp.put("height", "480");
        kvp.put("format", "image/png");
        final URL url = GetMapKvpRequestReader.class.getResource("BaseMapGroup.sld");
        // URLDecoder.decode fixes GEOS-3709
        kvp.put("sld", URLDecoder.decode(url.toString(), "UTF-8"));
        kvp.put("version", "1.1.1");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(kvp), kvp);

        assertEquals(1, request.getLayers().size());
        assertEquals(1, request.getStyles().size());
        assertEquals(getLayerId(MockData.BASIC_POLYGONS), request.getLayers().get(0).getName());
        Style expectedStyle = getCatalog().getStyleByName("polygon").getStyle();
        assertEquals(expectedStyle, request.getStyles().get(0));
    }

    public void testViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers", getLayerId(MockData.BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(1, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }
    
    public void testMultipleViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers", getLayerId(MockData.BASIC_POLYGONS) + "," + getLayerId(MockData.BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD,where:WHERE PERSONS > 10;str:FOO");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 10", viewParams.get("where"));
        assertEquals("FOO", viewParams.get("str"));
    }
    
    public void testFanOutViewParams() throws Exception {
        HashMap raw = new HashMap();
        raw.put("layers", getLayerId(MockData.BASIC_POLYGONS) + "," + getLayerId(MockData.BASIC_POLYGONS));
        raw.put("styles", "");
        raw.put("format", "image/jpeg");
        raw.put("srs", "epsg:3003");
        raw.put("bbox", "-10,-10,10,10");
        raw.put("height", "600");
        raw.put("width", "800");
        raw.put("request", "GetMap");
        raw.put("service", "wms");
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        GetMapRequest request = (GetMapRequest) reader.createRequest();
        request = (GetMapRequest) reader.read(request, parseKvp(raw), caseInsensitiveKvp(raw));

        List<Map<String, String>> viewParamsList = request.getViewParams();
        assertEquals(2, viewParamsList.size());
        Map viewParams = viewParamsList.get(0);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
        viewParams = viewParamsList.get(1);
        assertEquals("WHERE PERSONS > 1000000", viewParams.get("where"));
        assertEquals("ABCD", viewParams.get("str"));
    }
    
}
