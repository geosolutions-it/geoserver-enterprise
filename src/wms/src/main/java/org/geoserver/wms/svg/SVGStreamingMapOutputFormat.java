/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.io.IOException;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;

/**
 * Handles a GetMap request that expects a map in SVG format.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * @see StreamingSVGMap
 * @see SVGStreamingMapResponse
 */
public final class SVGStreamingMapOutputFormat implements GetMapOutputFormat {
    
    /** 
     * Default capabilities for SVG format.
     * 
     * <p>
     * <ol>
     *         <li>tiled = unsupported</li>
     *         <li>multipleValues = unsupported</li>
     *         <li>paletteSupported = unsupported</li>
     *         <li>transparency = supported</li>
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES= new MapProducerCapabilities(false, false, false, true, null);

    public SVGStreamingMapOutputFormat() {
        //
    }

    /**
     * @return {@code ["image/svg+xml", "image/svg xml", "image/svg"]}
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return SVG.OUTPUT_FORMATS;
    }

    /**
     * @return {@code "image/svg+xml"}
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return SVG.MIME_TYPE;
    }

    /**
     * 
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    public StreamingSVGMap produceMap(WMSMapContent mapContent) throws ServiceException,
            IOException {
        StreamingSVGMap svg = new StreamingSVGMap(mapContent);
        svg.setMimeType(getMimeType());
        return svg;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }

}
