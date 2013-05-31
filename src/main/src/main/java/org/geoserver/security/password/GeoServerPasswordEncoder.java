/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.io.IOException;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.security.authentication.encoding.PasswordEncoder;

/**
 * General Geoserver password encoding interface
 * 
 * @author christian
 *
 */
public interface GeoServerPasswordEncoder extends PasswordEncoder,BeanNameAware {

    public final static String PREFIX_DELIMTER=":";

    /**
     * Initialize this encoder.
     */
    void initialize(GeoServerSecurityManager securityManager) throws IOException;

    /**
     * Initialize this encoder for a {@link GeoServerUserGroupService} object.
     */
    void initializeFor(GeoServerUserGroupService service) throws IOException;

    /**
     * @return the {@link PasswordEncodingType} 
     */
    PasswordEncodingType getEncodingType();

    /**
     * The name of the password encoder.
     */
    String getName();
    
    /**
     * @param encPass
     * @return true if this encoder has encoded encPass
     */
    boolean isResponsibleForEncoding(String encPass);

    /**
     * Decodes an encoded password. Only supported for {@link PasswordEncodingType#ENCRYPT} and 
     * {@link PasswordEncodingType#PLAIN} encoders, ie those that return <code>true</code> from
     * {@link #isReversible()}. 
     * 
     * @param encPass The encoded password.
     * @throws UnsupportedOperationException
     */
    String decode(String encPass) throws UnsupportedOperationException;

    /**
     * Decodes an encoded password to a char array.
     * 
     * @see #decode(String)
     */
    char[] decodeToCharArray(String encPass) throws UnsupportedOperationException;

    /**
     * Encodes a raw password from a char array.
     *
     * @see #encodePassword(String, Object)
     */
    String encodePassword(char[] password, Object salt);

    /**
     * Validates a specified "raw" password (as char array) against an encoded password.
     * 
     * @see {@link #isPasswordValid(String, String, Object)
     */
    boolean isPasswordValid(String encPass, char[] rawPass, Object salt);

    /**
     * @return a prefix which is stored with the password.
     * This prefix must be unique within all {@link GeoServerPasswordEncoder}
     * implementations.
     * 
     * Reserved:
     * 
     * plain
     * digest1
     * crypt1
     * 
     * A plain text password is stored as
     * 
     * plain:password
     */
    String getPrefix();
    
    /**
     * Is this encoder available without installing
     * the unrestricted policy files of the java
     * cryptographic extension 
     * 
     * @return
     */
    boolean isAvailableWithoutStrongCryptogaphy();

    /**
     * Flag indicating if the encoder can decode an encrypted password back into its original 
     * plain text form.
     */
    boolean isReversible();
}
