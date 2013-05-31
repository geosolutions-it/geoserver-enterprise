/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;

/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs
 * as service exception in a <code>ows:ExceptionReport</code> document.
 * <p>
 * This service exception handler will generate an OWS exception report,
 * see {@linkplain http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd}.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @deprecated use {@link OWS10ServiceExceptionHandler}
 */
public class DefaultServiceExceptionHandler extends OWS10ServiceExceptionHandler {

    public DefaultServiceExceptionHandler() {
        super();
    }

    public DefaultServiceExceptionHandler(List services) {
        super(services);
    }
}
