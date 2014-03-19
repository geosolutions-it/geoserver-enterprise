/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.geotools.filter.v1_0.OGC;
import org.geotools.filter.v1_0.OGCConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.xml.sax.ContentHandler;

/**
 * Parses and encodes an OGC filter using ECQL
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FilterPPIO extends XMLPPIO {
    Configuration xml;

    protected FilterPPIO(Class type, String mimeType, QName element) {
        super(type, type, mimeType, element);
    }
    
    @Override
    public Object decode(Object input) throws Exception {
        if(input == null) {
            return null;
        } else if(input instanceof Filter) {
            return input;
        } else if(input instanceof String) {
            return decode(IOUtils.toInputStream((String) input)); 
        } else {
            throw new IllegalArgumentException("Cannot convert " + input + " into a Filter object");
        }
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = new Parser(xml);
        return p.parse(input);
    }

    @Override
    public void encode(Object obj, ContentHandler handler) throws Exception {
        Encoder e = new Encoder(xml);
        e.encode(obj, element, handler);
    }

    public static class Filter10 extends FilterPPIO {

        public Filter10() {
            super(Filter.class, "text/xml; subtype=filter/1.0", OGC.Filter);
            xml = new OGCConfiguration();
        }
    }

    public static class Filter11 extends FilterPPIO {

        public Filter11() {
            super(Filter.class, "text/xml; subtype=filter/1.1", org.geotools.filter.v1_1.OGC.Filter);
            xml = new org.geotools.filter.v1_1.OGCConfiguration();
        }
    }

}
