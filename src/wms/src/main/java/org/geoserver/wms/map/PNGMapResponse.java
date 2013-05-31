/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Transparency;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.kvp.PaletteManager;
import org.geoserver.wms.map.quantize.CachingColorIndexer;
import org.geoserver.wms.map.quantize.ColorIndexer;
import org.geoserver.wms.map.quantize.ColorIndexerDescriptor;
import org.geoserver.wms.map.quantize.LRUColorIndexer;
import org.geoserver.wms.map.quantize.Quantizer;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in GIF format.
 * 
 * @author Simone Giannecchini
 * @author Didier Richard
 * @version $Id
 */
public class PNGMapResponse extends RenderedImageMapResponse {
    /** Logger */
    private static final Logger LOGGER = Logging.getLogger(PNGMapResponse.class);

    private static final String MIME_TYPE = "image/png";

    private static final String MIME_TYPE_8BIT = "image/png; mode=8bit";

    private static final String[] OUTPUT_FORMATS = { MIME_TYPE, MIME_TYPE_8BIT, "image/png8" };

    /**
     * The two quantizers available for PNG images
     */
    public enum QuantizeMethod {
        Octree, MedianCut
    };

    static {
        ColorIndexerDescriptor.register();
    }

    /**
     * Default capabilities for PNG format.
     * 
     * <p>
     * <ol>
     * <li>tiled = supported</li>
     * <li>multipleValues = unsupported</li>
     * <li>paletteSupported = supported</li>
     * <li>transparency = supported</li>
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true, false,
            true, true, null);

    /**
     * @param format the format name as to be reported in the capabilities document
     * @param wms
     */
    public PNGMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetMapRequest request = (GetMapRequest) operation.getParameters()[0];
        if (request.getFormat().contains("8")) {
            return MIME_TYPE_8BIT;
        } else {
            return MIME_TYPE;
        }
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     * 
     * @see RasterMapOutputFormat#formatImageOutputStream(RenderedImage, OutputStream)
     */
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContent mapContent) throws ServiceException, IOException {
        // /////////////////////////////////////////////////////////////////
        //
        // Reformatting this image for png
        //
        // /////////////////////////////////////////////////////////////////
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ...");
        }
        
        // check to see if we have to see a translucent or bitmask quantizer
        image = applyPalette(image, mapContent, "image/png8", true);

        Boolean PNGNativeAcc = wms.getPNGNativeAcceleration();
        float quality = (100 - wms.getPngCompression()) / 100.0f;
        SampleModel sm = image.getSampleModel();
        int numBits = sm.getSampleSize(0);
        // png acceleration only works on 2 bit and 8 bit images, crashes on 4 bits
        boolean nativeAcceleration = PNGNativeAcc.booleanValue() && !(numBits > 1 && numBits < 8);
        ImageWorker iw = new ImageWorker(image);
        iw.writePNG(outStream, "FILTERED", quality, nativeAcceleration, false);
        RasterCleaner.addImage(iw.getRenderedImage());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing png image ... done!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }
}
