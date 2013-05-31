/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Base class that handles common behavior between 1.1.1 and 1.3.0
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
public abstract class BaseCapabilitiesResponse extends Response {

    private String mime;

    /**
     * @param binding
     */
    protected BaseCapabilitiesResponse(Class<?> binding, String mime) {
        super(binding);
        this.mime=mime;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return "getcapabilities_"+operation.getService().getVersion().toString()+".xml";
    }

    /**
     * @return {@code "text/xml"}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *      org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {
    
        if (value.getClass().isAssignableFrom(super.getBinding())) {
            return mime;
        }
    
        throw new IllegalArgumentException(value == null ? "null" : value.getClass().getName()
                + "/" + operation.getId());
    }

}