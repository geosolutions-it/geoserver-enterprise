/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

/*
 * Exception raised when a backup/restore operation is invoked
 * but the requested operation is not permitted
 *  
 * @author Luca Morandini lmorandini@ieee.org
 */
public class UnallowedOperationException extends Exception {

    UnallowedOperationException (String msg) {
        super(msg);
    }
}
