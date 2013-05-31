/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Does some post processing on a request to generate some bit of request information.
 * <p>
 * Typically this interface is used for tasks that are expensive to compute such as a 
 * reverse dns lookup or a geoip lookup.
 * </p>
 * <p>
 * Implementations of this class <b>must</b> be thread safe.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface RequestPostProcessor {

    /**
     * Post processes a request. 
     * <p>
     * This method should do whatever processing it needs to and then set any information
     * on <tt>data</tt> that is appropriate. There is no need for this method to persist the 
     * request object, it will be done after the post processing chain has been completed.  
     * </p>
     */
    void run(RequestData data, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
