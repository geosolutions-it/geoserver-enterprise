/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Abstract base class for all security filter configurations.
 * 
 * @author mcr
 */
public abstract class SecurityFilterConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;

    /**
     * Determines if the filter provides an {@link AuthenticationEntryPoint}.
     * <p>
     * If <code>true</code>, the corresponding {@link GeoServerSecurityFilter} class must return 
     * non-null from the method {@link GeoServerSecurityFilter#getAuthenticationEntryPoint()}.
     * </p>
     * 
     * @return true if the corresponding filter provides an
     * {@link AuthenticationEntryPoint} object. 
     */
    public boolean providesAuthenticationEntryPoint() {
        return false;
    }

}
