/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerDigestAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerDigestAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class DigestAuthFilterPanelInfo 
    extends AuthenticationFilterPanelInfo<DigestAuthenticationFilterConfig, DigestAuthFilterPanel>{

    public DigestAuthFilterPanelInfo() {
        setServiceClass(GeoServerDigestAuthenticationFilter.class);
        setServiceConfigClass(DigestAuthenticationFilterConfig.class);
        setComponentClass(DigestAuthFilterPanel.class);
    }
}
