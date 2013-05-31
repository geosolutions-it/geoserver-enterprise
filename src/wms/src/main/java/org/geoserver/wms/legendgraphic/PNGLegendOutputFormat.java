/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;

/**
 * PNG output format for the WMS {@link GetLegendGraphic} operation.
 * 
 * @author Gabriel Roldan
 * @author Justin Deoliveira
 * @version $Id: PNGLegendGraphicProducer.java 4776 2006-07-24 14:43:05Z afabiani $
 */
public class PNGLegendOutputFormat implements GetLegendGraphicOutputFormat {

    public static final String MIME_TYPE = "image/png";

    /**
     * Creates a new JAI based legend producer for creating <code>outputFormat</code> type images.
     */
    public PNGLegendOutputFormat() {
        //
    }

    /**
     * Builds and returns a {@link BufferedImageLegendGraphic} appropriate to be encoded as PNG
     * 
     * @see GetLegendGraphicOutputFormat#produceLegendGraphic(GetLegendGraphicRequest)
     */
    public BufferedImageLegendGraphic produceLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        BufferedImageLegendGraphicBuilder builder = new BufferedImageLegendGraphicBuilder();
        BufferedImage legendGraphic = builder.buildLegendGraphic(request);
        BufferedImageLegendGraphic legend = new BufferedImageLegendGraphic(legendGraphic);
        return legend;
    }

    /**
     * @return {@code "image/png"}
     * @see org.geoserver.wms.GetLegendGraphicOutputFormat#getContentType()
     */
    public String getContentType() throws IllegalStateException {
        return MIME_TYPE;
    }

}
