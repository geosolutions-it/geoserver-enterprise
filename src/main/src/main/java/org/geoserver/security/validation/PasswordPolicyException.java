/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.validation;

public class PasswordPolicyException extends AbstractSecurityException {
    private static final long serialVersionUID = 1L;

    public final static String IS_NULL="IS_NULL";
    //return MessageFormat.format("Password is mandatory",args);

    
    public final static String NO_DIGIT="NO_DIGIT";
    //return MessageFormat.format("Password does not contain a digit",args);
    
    
    public final static String NO_UPPERCASE="NO_UPPERCASE";
    //return MessageFormat.format("password does not contain an upper case letter",args);
    
    
    public final static String NO_LOWERCASE="NO_LOWERCASE";
    //return MessageFormat.format("password does not contain a lower case letter",args);
    

    public final static String MIN_LENGTH_$1="MIN_LENGTH";
    //return MessageFormat.format("password must have {0} characters",args);
    
    
    public final static String MAX_LENGTH_$1="MAX_LENGTH";
    //return MessageFormat.format("password has more than {0} characters",args);
    
    
    public final static String RESERVED_PREFIX_$1="RESERVED_PREFIX";
    //return MessageFormat.format("password  starts with reserved prefix {0}",args);
    

    public PasswordPolicyException(String errorId, Object[] args) {
        super(errorId, args);
    }

    public PasswordPolicyException(String errorId, String message, Object[] args) {
        super(errorId, message, args);
    } 
}
