/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import org.geotools.xml.Configuration;

/**
 * Parses a {@code FILTER} parameter assuming the filters sent are encoded as
 * per the OGC Filter Encoding v1.0.0 specification.
 * <p>
 * This kvp parser is meant to be configured in the spring context to parse
 * filters when a GetFeature request is sent conforming to the WFS 1.1 spec.
 * </p>
 * 
 * @author Gabriel Roldan
 */
public class Filter_1_1_0_KvpParser extends FilterKvpParser {
    @Override
    protected Configuration getParserConfiguration() {
        return new org.geotools.filter.v1_1.OGCConfiguration();
    }
}
