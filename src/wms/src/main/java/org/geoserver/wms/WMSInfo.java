/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.ServiceInfo;

/**
 * Configuration object for Web Map Service.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public interface WMSInfo extends ServiceInfo {

    enum WMSInterpolation {
        Nearest, Bilinear, Bicubic
    }

    /**
     * The watermarking configuration.
     */
    WatermarkInfo getWatermark();

    /**
     * Sets the watermarking configuration.
     */
    void setWatermark(WatermarkInfo watermark);

    WMSInterpolation getInterpolation();

    void setInterpolation(WMSInterpolation interpolation);

    /**
     * The srs's that the wms service supports.
     */
    List<String> getSRS();

    /**
     * Flag controlling whether the WMS service, for each layer, should declare a bounding box 
     * for every CRS supported, in it's capabilities document. 
     * <p>
     * By default the number of CRS's supported is huge which does not make this option practical.
     * This flag is only respected in cases there {@link #getSRS()} is non empty.
     * </p>
     */
    Boolean isBBOXForEachCRS();

    /**
     * Sets flag controlling whether the WMS service, for each layer, should declare a bounding box 
     * for every CRS supported.
     * 
     * @see #isBBOXForEachCRS()
     */
    void setBBOXForEachCRS(Boolean bboxForEachCRS);

    /**
     * The maximum search radius for GetFeatureInfo
     */
    int getMaxBuffer();

    /**
     * Sets the maximum search radius for GetFeatureInfo (if 0 or negative no maximum is enforced)
     */
    void setMaxBuffer(int buffer);

    /**
     * Returns the max amount of memory, in kilobytes, that each WMS request can allocate (each
     * output format will make a best effort attempt to respect it, but there are no guarantees)
     * 
     * @return the limit, or 0 if no limit
     */
    int getMaxRequestMemory();

    /**
     * Sets the max amount of memory, in kilobytes, that each WMS request can allocate. Set it to 0
     * if no limit is desired.
     */
    void setMaxRequestMemory(int max);

    /**
     * The max time, in seconds, a WMS request is allowed to spend rendering the map. Various output
     * formats will do a best effort to respect it (raster formats, for example, will account just
     * rendering time, but not image encoding time)
     */
    int getMaxRenderingTime();

    /**
     * Sets the max allowed rendering time, in seconds
     * 
     * @param maxRenderingTime
     */
    void setMaxRenderingTime(int maxRenderingTime);

    /**
     * The max number of rendering errors that will be tolerated before stating the rendering
     * operation failed by throwing a service exception back to the client
     */
    int getMaxRenderingErrors();

    /**
     * Sets the max number of rendering errors tolerated
     * 
     * @param maxRenderingTime
     */
    void setMaxRenderingErrors(int maxRenderingTime);
    
    /**
     * Defines the list of authority URLs for the root WMS layer
     * 
     * @return the list of WMS root layer's authority URLs
     */
    List<AuthorityURLInfo> getAuthorityURLs();

    /**
     * @return the list of identifiers for the WMS root layer
     */
    List<LayerIdentifierInfo> getIdentifiers();
}
