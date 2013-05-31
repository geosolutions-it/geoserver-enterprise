/* Copyright (c) 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.geoserver.wms.GetCapabilitiesRequest;

/**
 * 
 * @author Gabriel Roldan
 */
public class CapabilitiesXmlReaderTest extends TestCase {

    public void testParseXmlGetCapabilities() throws Exception {
        CapabilitiesXmlReader reader = new CapabilitiesXmlReader();

        String plainRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //
                + "<ogc:GetCapabilities xmlns:ogc=\"http://www.opengis.net/ows\" " //
                + "         xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "         version=\"1.2.0\" updateSequence=\"1\" " //
                + "        service=\"WMS\"> " //
                + "</ogc:GetCapabilities>";

        Reader input = new StringReader(plainRequest);

        Object read = reader.read(null, input, null);
        assertTrue(read instanceof GetCapabilitiesRequest);

        GetCapabilitiesRequest request = (GetCapabilitiesRequest) read;
        assertEquals("GetCapabilities", request.getRequest());
        assertEquals("1.2.0", request.getVersion());
        assertEquals("1", request.getUpdateSequence());
    }
}
