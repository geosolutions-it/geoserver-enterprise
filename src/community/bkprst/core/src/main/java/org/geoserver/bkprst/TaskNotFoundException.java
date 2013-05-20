/* 
 * Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.bkprst;

/**
 * Exception raised when a requested task is not in the task manager
 *  
 * @author Luca Morandini lmorandini@ieee.org
 */
public class TaskNotFoundException extends Exception {

    /** serialVersionUID */
    private static final long serialVersionUID = -2147181929963520740L;

    TaskNotFoundException (String msg) {
        super(msg);
    }
}
