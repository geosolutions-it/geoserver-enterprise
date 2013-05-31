/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.auth.AuthenticationCache;

/**
 * 
 * Filters implementing this interface my use an 
 * {@link AuthenticationCache} 
 *  
 * 
 * @author mcr
 *
 */
public interface AuthenticationCachingFilter {

    /**
     * Tries to extract a unique key for the principal
     * If this is not possible, return <code>null</code>
     * 
     * if the principal equals {@link GeoServerUser#ROOT_USERNAME) 
     * <code>null</code> must be returned. (Never cache this user)
     * 
     * For pre-authentication filters, the name of the
     * principal is sufficient. All other filters
     * should include some information derived from the 
     * credentials, otherwise an attacker could authenticate
     * using only the principal information.
     * 
     * As an example, the derived information could be
     * an md5 checksum of the credentials 
     * 
     * @param request
     * @return
     */
    public String getCacheKey(HttpServletRequest request);
}
