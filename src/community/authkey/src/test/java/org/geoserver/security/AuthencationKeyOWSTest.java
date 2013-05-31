package org.geoserver.security;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import junit.framework.Test;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wms.WMSInfo;
import org.w3c.dom.Document;

public class AuthencationKeyOWSTest extends GeoServerTestSupport {

    private static String adminKey;

    private static String citeKey;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("", "http://www.opengis.net/ogc");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // setup limited srs
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        gs.save(wms);
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        // setup some users
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,cite");
        props.store(new FileOutputStream(users), "");

        // setup their data access rights
        // namespace.layer.permission=role[,role2,...]
        File layers = new File(security, "layers.properties");
        props = new Properties();
        props.put("mode", "hidden");
        props.put("*.*.r", "NO_ONE");
        props.put("*.*.w", "NO_ONE");
        props.put("sf.*.r", "*");
        props.put("cite.*.r", "cite");
        props.put("cite.*.w", "cite");
        props.store(new FileOutputStream(layers), "");

        // setup the authentication keys
        File authkeys = new File(security, PropertyAuthenticationKeyMapper.AUTHKEYS_FILE);
        props = new Properties();
        adminKey = UUID.randomUUID().toString();
        citeKey = UUID.randomUUID().toString();
        props.put(adminKey, "admin");
        props.put(citeKey, "cite");
        props.store(new FileOutputStream(authkeys), "");
    }

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new AuthencationKeyOWSTest());
    }

    /**
     * Enable the Spring Security authentication filters, we want the test to be complete and
     * realistic
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        javax.servlet.Filter springSecurityFilters = (javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy");
        javax.servlet.Filter authKeyFilter = (javax.servlet.Filter) GeoServerExtensions
                .bean("authKeyFilter");
        return Arrays.asList(springSecurityFilters, authKeyFilter);
    }

    public void testAnonymousCapabilities() throws Exception {
        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0");
        // print(doc);

        // check we have the sf layers, but not the cite ones not the cdf ones
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc)
                .getLength() > 1);
        assertEquals(0, engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                .getLength());
        assertEquals(0, engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                .getLength());
    }

    public void testAdminCapabilities() throws Exception {
        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0&authkey=" + adminKey);
        // print(doc);

        // check we have all the layers
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc)
                .getLength() > 1);
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                .getLength() > 1);
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                .getLength() > 1);

        // check the authentication key has been propagated
        String url = engine.evaluate("//GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href", doc);
        assertTrue(url.contains("&authkey=" + adminKey));
    }

    public void testCiteCapabilities() throws Exception {
        Document doc = getAsDOM("wms?request=GetCapabilities&version=1.1.0&authkey=" + citeKey);
        // print(doc);

        // check we have the sf and cite layers, but not cdf
        XpathEngine engine = XMLUnit.newXpathEngine();
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'sf:')]", doc)
                .getLength() > 1);
        assertTrue(engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cite:')]", doc)
                .getLength() > 1);
        assertEquals(0, engine.getMatchingNodes("//Layer/Name[starts-with(text(), 'cdf:')]", doc)
                .getLength());

        // check the authentication key has been propagated
        String url = engine.evaluate("//GetMap/DCPType/HTTP/Get/OnlineResource/@xlink:href", doc);
        assertTrue(url.contains("&authkey=" + citeKey));
    }

    public void testAnonymousGetFeature() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                + getLayerId(MockData.PONDS));
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getLocalName());
    }

    public void testAdminGetFeature() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                + getLayerId(MockData.PONDS) + "&authkey=" + adminKey);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String url = engine.evaluate("//wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertTrue(url.contains("&authkey=" + adminKey));
    }

    public void testCiteGetFeature() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=1.0.0&request=GetFeature&typeName="
                + getLayerId(MockData.PONDS) + "&authkey=" + citeKey);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String url = engine.evaluate("//wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertTrue(url.contains("&authkey=" + citeKey));
    }
}
