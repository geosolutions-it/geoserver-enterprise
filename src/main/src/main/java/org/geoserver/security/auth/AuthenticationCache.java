/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.springframework.security.core.Authentication;

/**
 * Interface to cache {@link Authentication} objects.
 * <p>
 * </p>
 * The key is created from the name of the filter and the result of 
 * {@link AuthenticationCachingFilter#getCacheKey(javax.servlet.http.HttpServletRequest)}
 * 
 * @author mcr
 */
public interface AuthenticationCache {

    int DEFAULT_IDLE_TIME = 300;
    int DEFAULT_LIVE_TIME = 600;

    /**
     * Clears all cache entries
     */
    public void removeAll();
    /**
     * Clears all cache entries for filterName
     * 
     * @param filterName
     */
    public void removeAll(String filterName);

    
    /**
     * Clears a specific chache entry
     * 
     * @param filterName
     * @param cacheKey
     */
    public void remove(String filterName,String cacheKey);

    /**
     *  
     * 
     * @param filterName
     * @param cacheKey
     * @return
     */
    public Authentication get(String filterName, String cacheKey);
    
    /**
     * @param filterName
     * @param cacheKey
     * @param auth
     * @param timeToIdleSeconds (time to evict after last access)
     * @param timeToLiveSeconds (time to evict after creation time)
     */
    public void put(String filterName, String cacheKey, Authentication auth, 
            Integer timeToIdleSeconds, Integer timeToLiveSeconds);
    
    /**
     * timeToIdleSeconds and timeToLiveSeconds are derived
     * from the cache global settings
     * 
     * @param filterName
     * @param cacheKey
     * @param auth
     */
    public void put(String filterName, String cacheKey, Authentication auth);


}
