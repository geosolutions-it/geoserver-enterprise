/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.geoserver.security.ldap.LDAPAuthenticationProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanelInfo;

/**
 * Configuration panel extension for {@link LDAPAuthenticationProvider}.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthProviderPanelInfo 
    extends AuthenticationProviderPanelInfo<LDAPSecurityServiceConfig, LDAPAuthProviderPanel> {

    public LDAPAuthProviderPanelInfo() {
        setComponentClass(LDAPAuthProviderPanel.class);
        setServiceClass(LDAPAuthenticationProvider.class);
        setServiceConfigClass(LDAPSecurityServiceConfig.class);
    }
}
