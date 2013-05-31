/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;

import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * A class implementing this interface implements a backend for
 * user and group management. The store always operates on a
 * {@link GeoServerUserGroupService} object.
 * 
 * @author christian
 *
 */
public interface GeoServerUserGroupStore extends GeoServerUserGroupService {

    /**
     * Initializes itself from a service for future 
     * store modifications concerning this service 
     * 
     * @param service
     */
    void initializeFromService(GeoServerUserGroupService service) throws IOException;

    /**
     * discards all entries
     * 
     * @throws IOException
     */
    void clear() throws IOException;

    
    /**
     * Adds a user, the {@link GeoServerUser#getPassword()
     * returns the raw password
     * 
     * The method must use #getPasswordValidatorName() to
     * validate the raw password and
     * #getPasswordEncoderName() to
     * encode the password.
     * 
     * 
     * @param user
     */
    void addUser(GeoServerUser user) throws  IOException, PasswordPolicyException;

    /**
     * Updates a user
     * 
     * The method must be able to determine if
     * {@link GeoServerUser#getPassword() has changed
     * (reread from backend, check for a prefix, ...)
     * 
     * if the password has changed, it is a raw password
     * and the method must use #getPasswordValidatorName() to
     * validate the raw password and
     * #getPasswordEncoderName() to
     * 
     * encode the password.
     *  
     * @param user
     */
    void updateUser(GeoServerUser user)  throws IOException, PasswordPolicyException;

    /**
     * Removes the specified user 
     * @param user
     * @return
     */
    boolean removeUser(GeoServerUser user)  throws IOException;
    
    /**
     * Adds a group 
     * @param group
     */
    void addGroup(GeoServerUserGroup group)  throws IOException;

    /**
     * Updates a group 
     * @param group
     */
    void updateGroup(GeoServerUserGroup group)  throws IOException;

    /**
     * Removes the specified group. 
     * 
     * @param group
     * @return
     */
    boolean removeGroup(GeoServerUserGroup group)  throws IOException;


    /**
     * Synchronizes all changes with the backend store.On success, 
     * the associated {@link GeoServerUserGroupService} object should
     * be loaded
     */
    void store() throws IOException;

    /**
     * Associates a user with a group, on success
     * 
     * @param user
     * @param group
     */
    void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)  throws IOException;
    
    /**
     * Disassociates a user from a group, on success
     * 
     * 
     * @param user
     * @param group
     */
    void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)  throws IOException;

    /**
     * returns true if there are pending modifications
     * not written to the backend store
     * 
     * @return true/false
     */
    boolean isModified();
 
}