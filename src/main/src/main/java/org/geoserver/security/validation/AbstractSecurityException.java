/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.validation;

import org.geoserver.platform.exception.GeoServerException;

/**
 * Base class for exceptions used for validation errors 
 * 
 * 
 * @author christian
 *
 */
public class AbstractSecurityException extends GeoServerException {
    private static final long serialVersionUID = 1L;

    /**
     * errorid is a unique identifier, message is a 
     * default error description, args are message arguments
     * to be used for an alternative message (i18n) 
     * 
     * @param errorId 
     * @param message
     * @param args 
     */
    public AbstractSecurityException(String errorId ,String message, Object... args) {        
        super(message);
        setId(errorId);
        setArgs(args);
    }

    public AbstractSecurityException(String errorId , Object... args) {
        this(errorId, errorId, args);
    }

    /**
     * @deprecated use {@link #getId()}
     */
    public String getErrorId() {
        return getId();
    }
}
