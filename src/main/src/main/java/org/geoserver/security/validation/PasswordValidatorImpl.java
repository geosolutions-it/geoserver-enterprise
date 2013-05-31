/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.validation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;

import static org.geoserver.security.validation.PasswordPolicyException.*;

/**
 * Implementation of the password {@link PasswordValidator} interface
 * 
 * @author christian
 *
 */
public class PasswordValidatorImpl extends AbstractSecurityValidator implements PasswordValidator {

    protected PasswordPolicyConfig config;
    protected static Set<String> notAllowedPrefixes;
    protected static Object  lock = new Object();
    
    /**
     * Calculates not allowed prefixes
     */
    public PasswordValidatorImpl(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }
    
    public static Set<String> getNotAllowedPrefixes() {
        if (notAllowedPrefixes!=null)
            return notAllowedPrefixes;
        synchronized (lock) {
            if (notAllowedPrefixes!=null)
                return notAllowedPrefixes;
            
            notAllowedPrefixes = new HashSet<String>();
            for (GeoServerPasswordEncoder enc : GeoServerExtensions.extensions(
                    GeoServerPasswordEncoder.class)) {
                notAllowedPrefixes.add(enc.getPrefix()+GeoServerPasswordEncoder.PREFIX_DELIMTER);
            }    
            return notAllowedPrefixes;
        }
    }

    /**
     * Checks if the password starts with an encoder prefix, if true
     * return the prefix, if false return <code>null</code>
     * 
     * @param password
     * @return
     */
    public static String  passwordStartsWithEncoderPrefix (char[] password) {
        
        if (password==null)
            return null;
        
        O:  for (String prefix: getNotAllowedPrefixes()) {
                if (prefix.length() > password.length) 
                    continue;
                for (int i = 0; i < prefix.length(); i++) {
                    if (prefix.charAt(i) != password[i]) 
                       continue O;
                }
                return prefix;    
            }
            return null;
        }   

        
    
    @Override
    public void setConfig(PasswordPolicyConfig config) {
        this.config=config;
    }

    @Override
    public PasswordPolicyConfig getConfig() {
        return config;
    }

    @Override
    public void validatePassword(char[] password) throws PasswordPolicyException {
        //if (password==null)
        //    throw createSecurityException(PW_IS_NULL); 

        if (password == null) {
            //treat as "empty"
            password = new char[]{};
        }
        
        if (password.length < config.getMinLength())
            throw createSecurityException(MIN_LENGTH_$1, config.getMinLength());
        
        if (config.getMaxLength() >=0 &&  password.length >config.getMaxLength())
            throw createSecurityException(MAX_LENGTH_$1,config.getMaxLength());

        if (config.isDigitRequired()) {
            if (checkUsingMethod("isDigit", password)==false)
                throw createSecurityException(NO_DIGIT);
        }
        if (config.isUppercaseRequired()) {
            if (checkUsingMethod("isUpperCase", password)==false)
                throw createSecurityException(NO_UPPERCASE);
        }
        if (config.isLowercaseRequired()) {
            if (checkUsingMethod("isLowerCase", password)==false)
                throw createSecurityException(NO_LOWERCASE);
        }    
        
        String prefix = passwordStartsWithEncoderPrefix(password); 
        if (prefix!=null)
            throw createSecurityException(RESERVED_PREFIX_$1,prefix);
    }
    
    /**
     * Executes statis check methods from the character class
     * 
     * @param methodname
     * @param charArray
     * @return
     */
    protected boolean checkUsingMethod(String methodname, char[] charArray) {
        try {
            Method m = getClass().getMethod(methodname, Character.class);
            for (char c : charArray) {
                Boolean result = (Boolean) m.invoke(this, c);
                if (result)
                    return true;
            }        
            return false;
        } catch (Exception ex) {
            throw new RuntimeException("never should reach this point",ex);
        }
    }

    public boolean isDigit(Character c) {
        return Character.isDigit(c);
    }
    public boolean isUpperCase(Character c) {
        return Character.isUpperCase(c);
    }

    public boolean isLowerCase(Character c) {
        return Character.isLowerCase(c);
    }

    /**
     * Helper method for creating a proper {@link PasswordPolicyException} object
     */
    protected PasswordPolicyException createSecurityException (String errorid, Object ...args) {
        PasswordPolicyException ex =  new PasswordPolicyException(errorid,args);
        return ex;
    }

}
