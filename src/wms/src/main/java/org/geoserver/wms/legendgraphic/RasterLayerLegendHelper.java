/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.ColorMapLegendCreator.Builder;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Helper class to create legends for raster styles by parsing the rastersymbolizer element.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@SuppressWarnings("deprecation")
public class RasterLayerLegendHelper {

    /**
     * The default legend is a simpe image with an R within it which stands for Raster.
     */
    private final static BufferedImage defaultLegend;
    static {
        BufferedImage imgShape = null;
        try {
            final File stylesDirectory = GeoserverDataDirectory.findCreateConfigDir("styles");
            final File rasterLegend = new File(stylesDirectory, "rasterLegend.png");
            if (rasterLegend.exists())
                imgShape = ImageIO.read(rasterLegend);
        } catch (Throwable e) {
            imgShape = null;
        }
        // set the default legend
        defaultLegend = imgShape;
    }

    private BufferedImage image;

    private RasterSymbolizer rasterSymbolizer;

    private int width;

    private int height;

    private boolean transparent;

    private Color bgColor;

    private ColorMapLegendCreator cMapLegendCreator;
    
    
    /**
     * Constructor for a RasterLayerLegendHelper.
     * 
     * <p>
     * It takes a {@link GetLegendGraphicRequest} object in order to do its magic.
     * 
     * @param request
     *            the {@link GetLegendGraphicRequest} for which we want to build a legend
     * @param style the {@link Style} for which we want to build a legend
     * @param rule the {@link Rule} to use for rendering
     */
    public RasterLayerLegendHelper(final GetLegendGraphicRequest request,Style style,String ruleName) {
        PackagedUtils.ensureNotNull(request, "The provided GetLegendGraphicRequest is null");
        
        parseRequest(request,style,ruleName);
    }

    @SuppressWarnings("unchecked")
    private void parseRequest(final GetLegendGraphicRequest request,Style gt2Style,String ruleName) {
        // get the requested layer
        // and check that it is actually a grid
        // final FeatureType layer = request.getLayer();
        // boolean found =false;
        // found = LegendUtils.checkGridLayer(layer);
        // if(!found)
        // throw new IllegalArgumentException("Unable to create legend for non raster style");

        
        final FeatureTypeStyle[] ftStyles = gt2Style.getFeatureTypeStyles();
        final double scaleDenominator = request.getScale();

        final Rule[] applicableRules;
        
        if (ruleName != null) {
            Rule rule = LegendUtils.getRule(ftStyles, ruleName);
            if (rule == null) {
                throw new ServiceException("Specified style does not contains a rule named " + ruleName);
            }
            applicableRules = new Rule[] {rule};
        } else {
            applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
        }
        // no rules means no legend has to be produced
        if (applicableRules.length != 0) {

//        final NumberRange<Double> scaleRange = NumberRange.create(scaleDenominator,
//                scaleDenominator);

        // get width and height
        width = request.getWidth();
        height = request.getHeight();
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException(
                    "Invalid width and or height for the GetLegendGraphicRequest");

        final Symbolizer[] symbolizers = applicableRules[0].getSymbolizers();
        if (symbolizers == null || symbolizers.length != 1 | symbolizers[0] == null)
            throw new IllegalArgumentException(
                    "Unable to create a legend for this style, we need exactly 1 Symbolizer");

        final Symbolizer symbolizer = symbolizers[0];
        if (!(symbolizer instanceof RasterSymbolizer))
            throw new IllegalArgumentException(
                    "Unable to create a legend for this style, we need a RasterSymbolizer");
        rasterSymbolizer = (RasterSymbolizer) symbolizer;

        // is background transparent?
        transparent = request.isTransparent();

        // background bkgColor
        bgColor = LegendUtils.getBackgroundColor(request);

        // colormap element
        final ColorMap cmap = rasterSymbolizer.getColorMap();
        final Builder cmapLegendBuilder = new ColorMapLegendCreator.Builder();
        if (cmap != null && cmap.getColorMapEntries() != null
                && cmap.getColorMapEntries().length > 0) {

            // passing additional options
            cmapLegendBuilder.setAdditionalOptions(request.getLegendOptions());

            // setting type of colormap
            cmapLegendBuilder.setColorMapType(cmap.getType());

            // is this colormap using extended colors
            cmapLegendBuilder.setExtended(cmap.getExtendedColors());

            // setting the requested colormap entries
            cmapLegendBuilder.setRequestedDimension(new Dimension(width, height));

            // setting transparency and background bkgColor
            cmapLegendBuilder.setTransparent(transparent);
            cmapLegendBuilder.setBackgroundColor(bgColor);

            // setting band

            // Setting label font and font bkgColor
            cmapLegendBuilder.setLabelFont(LegendUtils.getLabelFont(request));
            cmapLegendBuilder.setLabelFontColor(LegendUtils.getLabelFontColor(request));

            // set band
            final ChannelSelection channelSelection = rasterSymbolizer.getChannelSelection();
            cmapLegendBuilder.setBand(channelSelection != null ? channelSelection.getGrayChannel()
                    : null);

            // adding the colormap entries
            final ColorMapEntry[] colorMapEntries = cmap.getColorMapEntries();
            for (ColorMapEntry ce : colorMapEntries)
                if (ce != null)
                    cmapLegendBuilder.addColorMapEntry(ce);

            // check the additional options before proceeding
            cmapLegendBuilder.checkAdditionalOptions();

            // instantiate the creator
            cMapLegendCreator = cmapLegendBuilder.create();

        } else
            cMapLegendCreator = null;
        }

    }

    /**
     * Retrieves the legend for the provided request.
     * 
     * @return a {@link BufferedImage} that represents the legend for the provided request.
     */
    public BufferedImage getLegend() {
        if(rasterSymbolizer == null) {
            return null;
        }
        return createResponse();
    }

    private synchronized BufferedImage createResponse() {

        if (image == null) {

            if (cMapLegendCreator != null)

                // creating a legend
                image = cMapLegendCreator.getLegend();

            else {
                image = ImageUtils.createImage(width, height, (IndexColorModel) null, transparent);
                final Graphics2D graphics = ImageUtils.prepareTransparency(transparent, bgColor,
                        image, new HashMap<RenderingHints.Key, Object>());
                if (defaultLegend == null) {
                    drawRasterIcon(graphics);
                } else {
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.drawImage(defaultLegend, 0, 0, width, height, null);
                }
                graphics.dispose();
            }
        }

        return image;
    }

    /**
     * Builds a geosilk like raster icon
     */
    void drawRasterIcon(Graphics2D graphics) {
        Color blue = new Color(Integer.parseInt("5e72be", 16));
        Color cyan = new Color(Integer.parseInt("a2c0eb", 16));
        
        // color bg in blue
        graphics.setColor(blue);
        graphics.fillRect(0, 0, width, height);
        
        // create checkerboard with cyan
        graphics.setColor(cyan);
        for(int j = 0; j < height / 2; j++) {
            for(int i = (j % 2); i < width / 2; i+=2) {
                graphics.fillRect(i * 2, j * 2, 2, 2);
            }
        }
        
        // overlay a shade of blue that becomes more solid on the lower right corner
        GradientPaint paint = new GradientPaint(new Point(0,0), new Color(0, 0, 255, 0), 
                new Point(width, height), new Color(0, 0, 255, 100));
        graphics.setPaint(paint);
        graphics.fillRect(0, 0, width, height);
        
        // blue border
        graphics.setColor(Color.BLUE);
        graphics.drawRect(0, 0, width - 1, height -1 );
    }

}
