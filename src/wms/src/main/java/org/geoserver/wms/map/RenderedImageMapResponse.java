/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Transparency;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
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
import org.geoserver.wms.map.quantize.Quantizer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.image.palette.InverseColorMapOp;
import org.springframework.util.Assert;

/**
 * Abstract base class for GetMapProducers that relies in LiteRenderer for creating the raster map
 * and then outputs it in the format they specializes in.
 * 
 * <p>
 * This class does the job of producing a BufferedImage using geotools LiteRenderer, so it should be
 * enough for a subclass to implement {@linkplain #formatImageOutputStream}
 * </p>
 * 
 * <p>
 * Generates a map using the geotools jai rendering classes. Uses the Lite renderer, loading the
 * data on the fly, which is quite nice. Thanks Andrea and Gabriel. The word is that we should
 * eventually switch over to StyledMapRenderer and do some fancy stuff with caching layers, but I
 * think we are a ways off with its maturity to try that yet. So Lite treats us quite well, as it is
 * stateless and therefore loads up nice and fast.
 * </p>
 * 
 * <p>
 * </p>
 * 
 * @author Chris Holmes, TOPP
 * @author Simone Giannecchini, GeoSolutions
 * @version $Id$
 */
public abstract class RenderedImageMapResponse extends AbstractMapResponse {

    /** Which format to encode the image in if one is not supplied */
    private static final String DEFAULT_MAP_FORMAT = "image/png";

    /** WMS Service configuration * */
    protected final WMS wms;

    /**
     * 
     */
    public RenderedImageMapResponse(WMS wms) {
        this(DEFAULT_MAP_FORMAT, wms);
    }

    /**
     * @param the
     *            mime type to be written down as an HTTP header when a map of this format is
     *            generated
     */
    public RenderedImageMapResponse(String mime, WMS wms) {
        super(RenderedImageMap.class, mime);
        this.wms = wms;
    }

    public RenderedImageMapResponse(String[] outputFormats, WMS wms) {
        super(RenderedImageMap.class, outputFormats);
        this.wms = wms;
    }

    /**
     * Transforms a rendered image into the appropriate format, streaming to the output stream.
     * 
     * @param image
     *            The image to be formatted.
     * @param outStream
     *            The stream to write to.
     * 
     * @throws ServiceException
     * @throws IOException
     */
    public abstract void formatImageOutputStream(RenderedImage image, OutputStream outStream,
            WMSMapContent mapContent) throws ServiceException, IOException;

    /**
     * Writes the image to the given destination.
     * 
     * @param value
     *            must be a {@link RenderedImageMap}
     * @see GetMapOutputFormat#write(org.geoserver.wms.WebMap, OutputStream)
     * @see #formatImageOutputStream(RenderedImage, OutputStream, WMSMapContent)
     */
    @Override
    public final void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(RenderedImageMap.class, value);

        final RenderedImageMap imageMap = (RenderedImageMap) value;
        try {
            final RenderedImage image = imageMap.getImage();
            final List<GridCoverage2D> renderedCoverages = imageMap.getRenderedCoverages();
            final WMSMapContent mapContent = imageMap.getMapContext();
            try {
                formatImageOutputStream(image, output, mapContent);
                output.flush();
            } finally {
                // let go of the coverages created for rendering
                for (GridCoverage2D coverage : renderedCoverages) {
                    RasterCleaner.addCoverage(coverage);
                }
                RasterCleaner.addImage(image);
            }
        } finally {
            imageMap.dispose();
        }
    }
    
    /**
     * Applies a transformation to 8 bits + palette in case the user requested a specific palette or
     * the palette format has been requested, applying a bitmask or translucent palette inverter
     * according to the user request and the image structure

     * @param image
     * @param mapContent
     * @param palettedFormatName
     * @param supportsTranslucency If false the code will always apply the bitmask transformer
     * @return
     */
    protected RenderedImage applyPalette(RenderedImage image, WMSMapContent mapContent,
            String palettedFormatName, boolean supportsTranslucency) {
        // check to see if we have to see a translucent or bitmask quantizer
        GetMapRequest request = mapContent.getRequest();
        QuantizeMethod method = (QuantizeMethod) request.getFormatOptions().get(
                PaletteManager.QUANTIZER);
        boolean useBitmaskQuantizer = method == QuantizeMethod.Octree
                || !supportsTranslucency
                || (method == null && image.getColorModel().getTransparency() != Transparency.TRANSLUCENT);

        // do we have to use the bitmask quantizer?
        final String format = request.getFormat();
        IndexColorModel icm = mapContent.getPalette();
        if (useBitmaskQuantizer) {
            // user provided palette?
            if (icm != null) {
                image = forceIndexed8Bitmask(image, PaletteManager.getInverseColorMapOp(icm));
            } else if (palettedFormatName.equalsIgnoreCase(format)) {
                // or format that needs palette to be applied?
                image = forceIndexed8Bitmask(image, null);
            }
        } else {
            if (!(image.getColorModel() instanceof IndexColorModel)) {
                // try to force a RGBA setup
                image = new ImageWorker(image).rescaleToBytes().forceComponentColorModel()
                        .getRenderedImage();
                ColorIndexer indexer = null;
                
                // user provided palette?
                if (mapContent.getPalette() != null) {
                    indexer = new CachingColorIndexer(new LRUColorIndexer(icm, 1024));
                } else if (palettedFormatName.equalsIgnoreCase(format)) {
                    // build the palette and grab the optimized color indexer
                    indexer = new Quantizer(256).subsample().buildColorIndexer(image);
                }

                // if we have an indexer transform the image
                if (indexer != null) {
                    image = ColorIndexerDescriptor.create(image, indexer, null);
                }
            }
        }

        return image;
    }
    
    /**
     * @param originalImage
     * @return
     */
    protected RenderedImage forceIndexed8Bitmask(RenderedImage originalImage,
            InverseColorMapOp paletteInverter) {
        return ImageUtils.forceIndexed8Bitmask(originalImage, paletteInverter);
    }

    /**
     * Returns the capabilities for this output format 
     * @param outputFormat
     * @return
     */
    public abstract MapProducerCapabilities getCapabilities(String outputFormat);
}
