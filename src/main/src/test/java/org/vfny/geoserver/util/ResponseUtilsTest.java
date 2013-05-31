package org.vfny.geoserver.util;

import org.easymock.classextension.EasyMock;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationContext;

import junit.framework.TestCase;
import static org.easymock.EasyMock.*;

public class ResponseUtilsTest extends TestCase {

    void createAppContext(String proxyBaseUrl) {
        SettingsInfo settings = createNiceMock(SettingsInfo.class);
        expect(settings.getProxyBaseUrl()).andReturn(proxyBaseUrl).anyTimes();
        replay(settings);
        
        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getSettings()).andReturn(settings).anyTimes();
        replay(geoServer);
        
        ProxifyingURLMangler mangler = new ProxifyingURLMangler(geoServer);
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);
        expect(appContext.getBeanNamesForType(URLMangler.class)).andReturn(new String[]{"mangler"});
        expect(appContext.getBean("mangler")).andReturn(mangler).anyTimes();
        replay(appContext);
        new GeoServerExtensions().setApplicationContext(appContext);
    }

    public void testProxyMetadataURL() throws Exception {
        createAppContext("http://foo.org/geoserver");
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("http://bar.com/geoserver/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/gesoserver");
        assertEquals(link.getContent(), url);
    }

    public void testProxyMetadataURLBackReference() throws Exception {
        createAppContext("http://foo.org/geoserver");
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/gesoserver");
        assertEquals("http://foo.org/geoserver/metadata.xml?foo=bar", url);
    }

    public void testProxyMetadataURLBackReferenceNoProxyBaseUrl() throws Exception {
        createAppContext(null);
        MetadataLinkInfo link = new MetadataLinkInfoImpl();
        link.setContent("/metadata.xml?foo=bar");

        String url = ResponseUtils.proxifyMetadataLink(link, "http://localhost/geoserver");
        assertEquals("http://localhost/geoserver/metadata.xml?foo=bar", url);
    }
}
