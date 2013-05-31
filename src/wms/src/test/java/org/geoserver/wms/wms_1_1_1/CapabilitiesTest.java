/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.custommonkey.xmlunit.XMLUnit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CapabilitiesTest extends WMSTestSupport {

    public CapabilitiesTest() {
        super();
    }

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new CapabilitiesTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
        dataDirectory.disableDataStore(MockData.SF_PREFIX);
    }

    public void testCapabilities() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
    }

    public void testGetCapsContainsNoDisabledTypes() throws Exception {

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertEquals("WMT_MS_Capabilities", doc.getDocumentElement().getNodeName());

        // see that disabled elements are disabled for good
        assertXpathEvaluatesTo("0", "count(//Name[text()='sf:PrimitiveGeoFeature'])", doc);

    }

    public void testFilteredCapabilitiesCite() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1&namespace=cite"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//Layer/Name[starts-with(., cite)]", dom).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[not(starts-with(., cite))]", dom)
                .getLength());
    }

    public void testLayerCount() throws Exception {
        List<LayerInfo> layers = new ArrayList<LayerInfo>(getCatalog().getLayers());
        for (ListIterator<LayerInfo> it = layers.listIterator(); it.hasNext();) {
            LayerInfo next = it.next();
            if (!next.enabled() || next.getName().equals(MockData.GEOMETRYLESS.getLocalPart())) {
                it.remove();
            }
        }
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();

        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodeLayers = xpath.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/Layer",
                dom);

        assertEquals(layers.size() + groups.size(), nodeLayers.getLength());
    }

    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathExists("//Layer[Name='" + layerId + "']", dom);
            
            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            dom = dom(get("wms?request=getCapabilities&version=1.1.1"), true);
            assertXpathNotExists("//Layer[Name='" + layerId + "']", dom);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }
    
    public void testWorkspaceQualified() throws Exception {
        Document dom = dom(get("cite/wms?request=getCapabilities&version=1.1.1"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[starts-with(., 'cite')]", dom).getLength());
        assertTrue (xpath.getMatchingNodes("//Layer/Name[not(starts-with(., 'cite'))]", dom)
                .getLength() > 0 );

        NodeList nodes = xpath.getMatchingNodes("//Layer//OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/wms"));
        }

    }

    public void testLayerQualified() throws Exception {
        Document dom = dom(get("cite/Forests/wms?request=getCapabilities&version=1.1.1"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(0, xpath.getMatchingNodes("//Layer/Name[starts-with(., 'cite:Forests')]", dom)
                .getLength());
        assertEquals(1, xpath.getMatchingNodes("//Layer[Name = 'Forests']", dom)
                .getLength());

        NodeList nodes = xpath.getMatchingNodes("//Layer//OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            assertTrue(e.getAttribute("xlink:href").contains("geoserver/cite/Forests/wms"));
        }

    }

    public void testAttribution() throws Exception {
        // Uncomment the following lines if you want to use DTD validation for these tests
        // (by passing false as the second param to getAsDOM())
        // BUG: Currently, this doesn't seem to actually validate the document, although
        // 'validation' fails if the DTD is missing

        // GeoServerInfo global = getGeoServer().getGlobal();
        // global.setProxyBaseUrl("src/test/resources/geoserver");
        // getGeoServer().save(global);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo("0", "count(//Attribution)", doc);

        // Add attribution to one of the layers
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        AttributionInfo attr = points.getAttribution();

        attr.setTitle("Point Provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);

        // Add href to same layer
        attr = points.getAttribution();
        attr.setHref("http://example.com/points/provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/OnlineResource)", doc);

        // Add logo to same layer
        attr = points.getAttribution();
        attr.setLogoURL("http://example.com/points/logo");
        attr.setLogoType("image/logo");
        attr.setLogoHeight(50);
        attr.setLogoWidth(50);
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/Title)", doc);
        assertXpathEvaluatesTo("1", "count(//Attribution/LogoURL)", doc);
    }

    public void testAlternateStyles() throws Exception {
        // add an alternate style to Fifteen
        StyleInfo pointStyle = getCatalog().getStyleByName("point");
        LayerInfo layer = getCatalog().getLayerByName("Fifteen");
        layer.getStyles().add(pointStyle);
        getCatalog().save(layer);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//Layer[Name='cdf:Fifteen'])", doc);
        assertXpathEvaluatesTo("2", "count(//Layer[Name='cdf:Fifteen']/Style)", doc);

        XpathEngine xpath = newXpathEngine();
        String href = xpath
                .evaluate(
                        "//Layer[Name='cdf:Fifteen']/Style[Name='Default']/LegendURL/OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=Fifteen"));
        assertFalse(href.contains("style"));
        href = xpath
                .evaluate(
                        "//Layer[Name='cdf:Fifteen']/Style[Name='point']/LegendURL/OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=Fifteen"));
        assertTrue(href.contains("style=point"));
    }

    public void testServiceMetadata() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setTitle("test title");
        service.setAbstract("test abstract");
        service.setAccessConstraints("test accessConstraints");
        service.setFees("test fees");
        service.getKeywords().clear();
        service.getKeywords().add(new Keyword("test keyword 1"));
        service.getKeywords().add(new Keyword("test keyword 2"));
        service.setMaintainer("test maintainer");
        service.setOnlineResource("http://example.com/geoserver");
        GeoServerInfo global = getGeoServer().getGlobal();
        ContactInfo contact = global.getContact();
        contact.setAddress("__address");
        contact.setAddressCity("__city");
        contact.setAddressCountry("__country");
        contact.setAddressPostalCode("__ZIP");
        contact.setAddressState("__state");
        contact.setAddressType("__type");
        contact.setContactEmail("e@mail");
        contact.setContactOrganization("__org");
        contact.setContactFacsimile("__fax");
        contact.setContactPerson("__me");
        contact.setContactPosition("__position");
        contact.setContactVoice("__phone");
        
        getGeoServer().save(global);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        String base = "WMT_MS_Capabilities/Service/";
        assertXpathEvaluatesTo("OGC:WMS", base + "Name", doc);
        assertXpathEvaluatesTo("test title", base + "Title", doc);
        assertXpathEvaluatesTo("test abstract", base + "Abstract", doc);
        assertXpathEvaluatesTo("test keyword 1", base + "KeywordList/Keyword[1]", doc);
        assertXpathEvaluatesTo("test keyword 2", base + "KeywordList/Keyword[2]", doc);
        assertXpathEvaluatesTo("http://example.com/geoserver", base + "OnlineResource/@xlink:href", doc);
        
        String cinfo = base + "ContactInformation/";
        assertXpathEvaluatesTo("__me", cinfo + "ContactPersonPrimary/ContactPerson", doc);
        assertXpathEvaluatesTo("__org", cinfo + "ContactPersonPrimary/ContactOrganization", doc);
        assertXpathEvaluatesTo("__position", cinfo + "ContactPosition", doc);
        assertXpathEvaluatesTo("__type", cinfo + "ContactAddress/AddressType", doc);
        assertXpathEvaluatesTo("__address", cinfo + "ContactAddress/Address", doc);
        assertXpathEvaluatesTo("__city", cinfo + "ContactAddress/City", doc);
        assertXpathEvaluatesTo("__state", cinfo + "ContactAddress/StateOrProvince", doc);
        assertXpathEvaluatesTo("__ZIP", cinfo + "ContactAddress/PostCode", doc);
        assertXpathEvaluatesTo("__country", cinfo + "ContactAddress/Country", doc);
        assertXpathEvaluatesTo("__phone", cinfo + "ContactVoiceTelephone", doc);
        assertXpathEvaluatesTo("__fax", cinfo + "ContactFacsimileTelephone", doc);
        assertXpathEvaluatesTo("e@mail", cinfo + "ContactElectronicMailAddress", doc);
    }
    
    public void testQueryable() throws Exception{
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setQueryable(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setQueryable(false);
        getCatalog().save(points);        

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "//Layer[Name='" + linesName + "']/@queryable", doc);
        assertXpathEvaluatesTo("0", "//Layer[Name='" + pointsName + "']/@queryable", doc);
    }

}
