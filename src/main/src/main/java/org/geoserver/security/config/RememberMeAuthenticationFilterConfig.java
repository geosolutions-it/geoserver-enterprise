/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.GeoServerRememberMeAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * {@link GeoServerRememberMeAuthenticationFilter} configuration object.
 * <p>
 * The config is empty since {@link GeoServerSecurityManager} is used as {@link AuthenticationManager} 
 * and a global {@link RememberMeServices} object is in the Spring contex. 
 * </p>
 * @author mcr
 *
 */
public class RememberMeAuthenticationFilterConfig extends SecurityFilterConfig 
    implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;

}
