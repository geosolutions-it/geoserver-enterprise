package org.geoserver.wps;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;

import org.opengis.filter.Filter;
import org.w3c.dom.Document;

public class ResourceAccessManagerWPSTest extends WPSTestSupport {

    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }
    
    /**
     * Add the test resource access manager in the spring context
     */
    @Override
    protected String[] getSpringContextLocations() {
        String[] locations = super.getSpringContextLocations();
        String[] newLocations = Arrays.copyOf(locations, locations.length + 1);
        newLocations[newLocations.length - 1] = "classpath:/org/geoserver/wps/ResourceAccessManagerContext.xml"; 
        return newLocations;        
    }    
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // populate the access manager
        TestResourceAccessManager tam = (TestResourceAccessManager) applicationContext
                .getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings = catalog
                .getFeatureTypeByName(getLayerId(getTestData().BUILDINGS));

        // limits make the layer be visible when logged in as the cite user, but not when
        // running as the anonymous one (the TestResourceAccessManager does not allow
        // to run tests against un-recognized users)
        tam.putLimits("cite", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                Filter.INCLUDE, null, null));
        tam.putLimits("anonymous", buildings, new VectorAccessLimits(CatalogMode.HIDE, null,
                Filter.EXCLUDE, null, null));
    }

    
    public void testDenyAccess() throws Exception {
        Document dom = runBuildingsRequest();
        // print(dom);
        
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("0", xp.evaluate("count(//wps:ProcessSucceded)", dom));
    }
       
    public void testAllowAccess() throws Exception {
        authenticate("cite", "cite");
        Document dom = runBuildingsRequest();
        // print(dom);
        
        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
        assertEquals("8.0E-4 5.0E-4", xp.evaluate("//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:LowerCorner", dom));
        assertEquals("0.0024 0.0010", xp.evaluate("//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:UpperCorner", dom));
    }

    private Document runBuildingsRequest() throws Exception {
        // @formatter:off
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                + "            <wfs:Query typeName=\""
                + getLayerId(MockData.BUILDINGS)
                + "\"/>\n"
                + "          </wfs:GetFeature>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:Output>\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:Output>\n"
                + " </wps:ResponseForm>\n" 
                + "</wps:Execute>";
        // @formatter:on
        
        Document dom = postAsDOM("wps", xml);
        return dom;
    }
}