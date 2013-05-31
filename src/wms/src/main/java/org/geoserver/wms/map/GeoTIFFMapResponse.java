/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Transparency;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.ImageOutputStream;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.kvp.PaletteManager;
import org.geoserver.wms.map.PNGMapResponse.QuantizeMethod;
import org.geoserver.wms.map.quantize.CachingColorIndexer;
import org.geoserver.wms.map.quantize.ColorIndexer;
import org.geoserver.wms.map.quantize.ColorIndexerDescriptor;
import org.geoserver.wms.map.quantize.LRUColorIndexer;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.io.ImageIOExt;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.util.logging.Logging;

/**
 * Map response handler for GeoTiff output format. It basically relies on the GeoTiff module of
 * geotools.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 */
public class GeoTIFFMapResponse extends RenderedImageMapResponse {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(GeoTIFFMapResponse.class);

    private static final String IMAGE_GEOTIFF = "image/geotiff";

    private static final String IMAGE_GEOTIFF8 = "image/geotiff8";

    private static final String[] OUTPUT_FORMATS = { IMAGE_GEOTIFF, IMAGE_GEOTIFF8 };

    /** GridCoverageFactory. */
    private final static GridCoverageFactory factory = CoverageFactoryFinder
            .getGridCoverageFactory(null);
    
    /** 
     * Default capabilities for GEOTIFF.
     * 
     * <p>
     * <ol>
     *         <li>tiled = supported</li>
     *         <li>multipleValues = unsupported</li>
     *         <li>paletteSupported = supported</li>
     *         <li>transparency = supported</li>
     * </ol>
     * 
     * <p>
     * We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true, false, true, true, null);

    public GeoTIFFMapResponse(final WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    @Override
    public void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContent mapContent) throws ServiceException, IOException {
        // tiff
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing tiff image ...");
        }

        // do we want it to be 8 bits?
        image = applyPalette(image, mapContent, IMAGE_GEOTIFF8, false);
        
        // crating a grid coverage
        final GridCoverage2D gc = factory.create("geotiff", image,
                new GeneralEnvelope(mapContent.getRenderingArea()));

        // writing it out
        final ImageOutputStream imageOutStream = ImageIOExt.createImageOutputStream(image, outStream);
        if (imageOutStream == null) {
            throw new ServiceException("Unable to create ImageOutputStream.");
        }

        GeoTiffWriter writer = null;

        // write it out
        try {
            writer = new GeoTiffWriter(imageOutStream);
            writer.write(gc, null);
        } finally {
            try {
                imageOutStream.close();
            } catch (Throwable e) {
                // eat exception to release resources silently
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.log(Level.FINEST, "Unable to properly close output stream", e);
            }

            try {
                if (writer != null)
                    writer.dispose();
            } catch (Throwable e) {
                // eat exception to release resources silently
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.log(Level.FINEST, "Unable to properly dispose writer", e);
            }
            
            // let go of the chain behind the coverage
            RasterCleaner.addCoverage(gc);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing tiff image done!");
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }
}
