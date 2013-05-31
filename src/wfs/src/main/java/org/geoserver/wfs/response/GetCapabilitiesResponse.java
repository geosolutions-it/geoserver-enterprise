/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import net.opengis.wfs.GetCapabilitiesType;

import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.xml.transform.TransformerBase;


public class GetCapabilitiesResponse extends Response {
    public GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }
    
    /**
     * Makes sure this triggers only
     * </p>
     */
    public boolean canHandle(Operation operation) {
        // is this a wfs capabilities request?
        return "GetCapabilities".equalsIgnoreCase(operation.getId()) && 
                operation.getService().getId().equals("wfs");
    }

    public String getMimeType(Object value, Operation operation) {
        GetCapabilitiesRequest request = GetCapabilitiesRequest.adapt(operation.getParameters()[0]);

        if ((request != null) && (request.getAcceptFormats() != null)) {
            //look for an accepted format
            List formats = request.getAcceptFormats();

            for (Iterator f = formats.iterator(); f.hasNext();) {
                String format = (String) f.next();

                if (format.endsWith("/xml")) {
                    return format;
                }
            }
        }

        //default
        return "application/xml";
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException {
        TransformerBase tx = (TransformerBase) value;

        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
