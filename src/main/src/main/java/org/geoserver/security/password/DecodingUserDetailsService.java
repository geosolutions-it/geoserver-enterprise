/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.io.IOException;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Wrapper class for a {@link GeoServerUserGroupService} object
 * decoding the passwords.
 * 
 * This is needed for some authentication mechanisms, HTTP Digest
 * authentication as an example.
 * 
 * Decoding is only possible for {@link GeoServerUserPasswordEncoder}
 * objects of type {@link PasswordEncodingType#PLAIN} or
 * {@link PasswordEncodingType#ENCRYPT} 
 * 
 * @author christian
 *
 */
public class DecodingUserDetailsService implements UserDetailsService {

    protected GeoServerUserGroupService service;
    protected GeoServerMultiplexingPasswordEncoder encoder;
    
    /**
     * Creates a new Instance
     * @param service
     * @return
     * @throws IOException
     */
    public static DecodingUserDetailsService newInstance(GeoServerUserGroupService service) throws IOException {
        DecodingUserDetailsService decodingService = new DecodingUserDetailsService();
        decodingService.setGeoserverUserGroupService(service);        
        return decodingService;
    }
     
    /**
     * Protected, use {@link #canBeUsedFor(GeoServerUserGroupService)} followed
     * by {@link #newInstance(GeoServerUserGroupService)}
     */
    protected DecodingUserDetailsService() {        
    }
    
    /**
     * sets the wrapped {@link GeoServerUserGroupService} objects
     * and prepares the {@link GeoServerUserPasswordEncoder}
     * 
     * @param service
     * @throws IOException
     */
    public void setGeoserverUserGroupService(GeoServerUserGroupService service) throws IOException {
        this.service=service;
        encoder=new GeoServerMultiplexingPasswordEncoder(service.getSecurityManager(),service);
    }
    
    /**
     * loads the user and decodes the password to plain text (if possible).
     * 
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
            DataAccessException {
        GeoServerUser user = (GeoServerUser) service.loadUserByUsername(username);
        if (user==null) return null;
        try {
            String decoded = encoder.decode(user.getPassword());
            return new UserDetailsPasswordWrapper(user, decoded);
        } catch (UnsupportedOperationException ex) {
            return new UserDetailsPasswordWrapper(user, user.getPassword());
        }
    }

}
