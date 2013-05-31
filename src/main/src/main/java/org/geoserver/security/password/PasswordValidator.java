/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.password;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.PasswordValidatorImpl;

/**
 * Validates a password based on 
 * {@link PasswordPolicyConfig} object
 * 
 * At a bare minimum, <code>null</code> passwords
 * should not be allowed.
 * 
 * Additionally, password must not start with 
 * prefixes used by the {@link GeoServerPasswordEncoder} objects
 * To get the prefixes use
 * 
 * <code>
 * for (GeoserverPasswordEncoder enc : GeoServerExtensions.extensions(
 *           GeoserverPasswordEncoder.class)) {
 *     System.out.println(enc.getPrefix()+GeoserverPasswordEncoder.PREFIX_DELIMTER);
 *         }
 * </code>
 * 
 * A concrete example can be found in
 * {@link PasswordValidatorImpl#PasswordValidatorImpl()}
 * 
 * @author christian
 *
 */
public interface PasswordValidator {
    
    public final static String DEFAULT_NAME="default";
    public final static String MASTERPASSWORD_NAME="master";
    
    /**
     * Setter for the config
     */
    void setConfig(PasswordPolicyConfig config);
    
    /**
     * Getter for the config
     */
    PasswordPolicyConfig getConfig();
    
    /**
     * Validates the password, throws an exception if the password is not valid
     */
    void validatePassword(char[] password) throws PasswordPolicyException;

}
