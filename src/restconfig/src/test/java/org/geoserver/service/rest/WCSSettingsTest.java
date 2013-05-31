package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WCSSettingsTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        geoServer = GeoServerExtensions.bean(GeoServer.class, applicationContext);
    }

    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wcs/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wcsinfo.get("id"));
        assertEquals("true", wcsinfo.get("enabled").toString().trim());
        assertEquals("My GeoServer WCS", wcsinfo.get("name"));
        assertEquals("false", wcsinfo.get("verbose").toString().trim());
    }

    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wcs/settings.xml");
        assertEquals("wcs", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("My GeoServer WCS", "/wcs/name", dom);
        assertXpathEvaluatesTo("false", "/wcs/verbose", dom);
    }

    public void testPutAsJSON() throws Exception {
        String json = "{'wcs': {'id':'wcs','enabled':'false','name':'WCS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wcs/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wcsinfo.get("id"));
        assertEquals("false", wcsinfo.get("enabled").toString().trim());
        assertEquals("WCS", wcsinfo.get("name"));
    }

    public void testPutASXML() throws Exception {
        String xml = "<wcs>"
                + "<id>wcs</id>"
                + "<enabled>false</enabled>"
                + "<name>WCS</name><title>GeoServer Web Coverage Service</title>"
                + "<maintainer>http://jira.codehaus.org/secure/BrowseProject.jspa?id=10311</maintainer>"
                + "</wcs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/settings", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wcs/settings.xml");
        assertXpathEvaluatesTo("false", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("WCS", "/wcs/name", dom);
    }

    public void testDelete() throws Exception {
        assertEquals(405, deleteAsServletResponse("/rest/services/wcs/settings").getStatusCode());
    }
}
