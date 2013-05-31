/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;


/**
 * Configuration for security context persistence
 * 
 * if {@link #allowSessionCreation} is <code>true</code>, 
 * creation of a {@link HttpSession} object is allowed
 * and an {@link Authentication} object can be stored to 
 * avoid re-authentication fore each request
 * 
 * Should be <code>false</code> for stateless services  
 * 
 * @author mcr
 *
 */
public class SecurityContextPersistenceFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;
    private boolean allowSessionCreation;
    
    public boolean isAllowSessionCreation() {
        return allowSessionCreation;
    }
    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }
    
    
        
}
