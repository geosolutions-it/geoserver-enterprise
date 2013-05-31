/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.xml.sax.ContentHandler;


public class GML3GeometryTranslator extends GeometryTranslator {
    public GML3GeometryTranslator(ContentHandler handler) {
        super(handler);
    }

    public GML3GeometryTranslator(ContentHandler handler, int numDecimals, boolean useDummyZ) {
        super(handler, numDecimals, useDummyZ);
    }

    public GML3GeometryTranslator(ContentHandler handler, int numDecimals) {
        super(handler, numDecimals);
    }

    protected String boxName() {
        return "Envelope";
    }

    protected void encodeNullBounds() {
        element("Null", null);
    }
}
