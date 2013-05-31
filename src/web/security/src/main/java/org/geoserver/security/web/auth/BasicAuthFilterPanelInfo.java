/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerBasicAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class BasicAuthFilterPanelInfo 
    extends AuthenticationFilterPanelInfo<BasicAuthenticationFilterConfig, BasicAuthFilterPanel>{

    public BasicAuthFilterPanelInfo() {
        setServiceClass(GeoServerBasicAuthenticationFilter.class);
        setServiceConfigClass(BasicAuthenticationFilterConfig.class);
        setComponentClass(BasicAuthFilterPanel.class);
    }
}
