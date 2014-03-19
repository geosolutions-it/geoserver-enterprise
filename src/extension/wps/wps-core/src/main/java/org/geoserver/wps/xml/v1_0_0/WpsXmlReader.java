/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.xml.v1_0_0;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wps.WPSException;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPSConfiguration;
import org.geotools.xml.Parser;

/**
 * WPS XML parser
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class WpsXmlReader extends XmlRequestReader {
    public Logger LOGGER = Logging.getLogger("org.geoserver.wps");

    private WPSConfiguration configuration;

    public WpsXmlReader(String element, String version, WPSConfiguration configuration) {
        super(new QName(org.geotools.wps.WPS.NAMESPACE, element), new Version("1.0.0"), "wps");
        this.configuration = configuration;
    }

    @SuppressWarnings("unchecked")
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        Parser parser = new Parser(configuration);
        parser.setValidating(true);

        Object parsed;
        try {
            parsed = parser.parse(reader);
        } catch(Exception e) {
            throw new WPSException("Could not parse XML request.", e);
        }

        if (!parser.getValidationErrors().isEmpty()) {
            WPSException exception = new WPSException("Invalid request", "InvalidParameterValue");

            for(Exception error : (List<Exception>)parser.getValidationErrors()) {
                LOGGER.warning( error.getLocalizedMessage() );
                exception.getExceptionText().add(error.getLocalizedMessage());
            }
        }

        return parsed;
    }
}
