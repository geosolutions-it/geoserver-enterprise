package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerHomePageContentProvider;

public class ClusterHomePageContentProvider implements GeoServerHomePageContentProvider {

    private final JMSConfiguration config;

    public ClusterHomePageContentProvider(JMSConfiguration config) {
        this.config = config;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        GeoServerSecurityManager secMgr = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        if (secMgr.checkAuthenticationForAdminRole()) { 
            return new NodePanel(id, config);
        }
        return new WebMarkupContainer(id);
    }

}
