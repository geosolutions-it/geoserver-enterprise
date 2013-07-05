/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DpiRescaleStyleVisitor;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.geotools.util.NumberRange;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Template {@linkPlain org.vfny.geoserver.responses.wms.GetLegendGraphicProducer} based on
 * GeoTools' {@link GeoTools' {@link http
 * ://svn.geotools.org/geotools/trunk/gt/module/main/src/org/geotools
 * /renderer/lite/StyledShapePainter.java StyledShapePainter} that produces a BufferedImage with the
 * appropiate legend graphic for a given GetLegendGraphic WMS request.
 * 
 * <p>
 * It should be enough for a subclass to implement {@linkPlain
 * org.vfny.geoserver.responses.wms.GetLegendGraphicProducer#writeTo(OutputStream)} and
 * <code>getContentType()</code> in order to encode the BufferedImage produced by this class to the
 * appropiate output format.
 * </p>
 * 
 * <p>
 * This class takes literally the fact that the arguments <code>WIDTH</code> and <code>HEIGHT</code>
 * are just <i>hints</i> about the desired dimensions of the produced graphic, and the need to
 * produce a legend graphic representative enough of the SLD style for which it is being generated.
 * Thus, if no <code>RULE</code> parameter was passed and the style has more than one applicable
 * Rule for the actual scale factor, there will be generated a legend graphic of the specified
 * width, but with as many stacked graphics as applicable rules were found, providing by this way a
 * representative enough legend.
 * </p>
 * 
 * @author Gabriel Roldan
 * @author Simone Giannecchini, GeoSolutions SAS
 * @version $Id$
 */
public class BufferedImageLegendGraphicBuilder {

    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;

    /**
     * Singleton shape painter to serve all legend requests. We can use a single shape painter
     * instance as long as it remains thread safe.
     */
    private static final StyledShapePainter shapePainter = new StyledShapePainter();

    /**
     * used to create sample point shapes with LiteShape (not lines nor polygons)
     */
    private static final GeometryFactory geomFac = new GeometryFactory();

    /**
     * Just a holder to avoid creating many polygon shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleRect;

    /**
     * Just a holder to avoid creating many line shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;

    /**
     * Just a holder to avoid creating many point shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 samplePoint;

    /**
     * Default minimum size for symbols rendering.
     * Can be overridden using LEGEND_OPTIONS (minSymbolSize).
     */
    private final double MINIMUM_SYMBOL_SIZE = 3.0;
    
    /**
     * Default constructor. Subclasses may provide its own with a String parameter to establish its
     * desired output format, if they support more than one (e.g. a JAI based one)
     */
    public BufferedImageLegendGraphicBuilder() {
        super();
    }

    /**
     * Takes a GetLegendGraphicRequest and produces a BufferedImage that then can be used by a
     * subclass to encode it to the appropiate output format.
     * 
     * @param request
     *            the "parsed" request, where "parsed" means that it's values are already validated
     *            so this method must not take care of verifying the requested layer exists and the
     *            like.
     * @return
     * 
     * @throws ServiceException
     *             if there are problems creating a "sample" feature instance for the FeatureType
     *             <code>request</code> returns as the required layer (which should not occur).
     */
    public BufferedImage buildLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        // list of images to be rendered for the layers (more than one if
        // a layer group is given)
        List<RenderedImage> layersImages=new ArrayList<RenderedImage>();
        
        
        List<FeatureType> layers=request.getLayers();
        List<Style> styles=request.getStyles();
        List<String> rules=request.getRules();
        
        
        boolean forceLabelsOn = false;
        boolean forceLabelsOff = false;
        if (request.getLegendOptions().get("forceLabels") instanceof String) {
            String forceLabelsOpt = (String) request.getLegendOptions().get("forceLabels");
            if (forceLabelsOpt.equalsIgnoreCase("on")) {
                forceLabelsOn = true;
            } else if (forceLabelsOpt.equalsIgnoreCase("off")) {
                forceLabelsOff = true;
            }
        }
                
        boolean forceTitlesOff = false;
        if (request.getLegendOptions().get("forceTitles") instanceof String) {
            String forceTitlesOpt = (String) request.getLegendOptions().get("forceTitles");
            if (forceTitlesOpt.equalsIgnoreCase("off")) {
                forceTitlesOff = true;
            }
        }
        
        for (int pos = 0; pos < layers.size(); pos++) {
            // current layer
            FeatureType layer=layers.get(pos);
            
            // style and rule to use for the current layer
            Style gt2Style = styles.get(pos);
            String ruleName = null;
            
            if (gt2Style == null) {
                throw new NullPointerException("request.getStyle()");
            }
                        
            // get rule corresponding to the layer index
            if(rules.size() > 0) {
                ruleName = rules.get(pos);
            }
            // normalize to null for NO RULE
            if(ruleName != null && ruleName.equals("")) {
                ruleName = null;
            }
            
            // width and height, we might have to rescale those in case of DPI usage            
            int w = request.getWidth();
            int h = request.getHeight();

            // apply dpi rescale
            double dpi = RendererUtilities.getDpi(request.getLegendOptions());
            double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
            if(dpi != standardDpi) {
                double scaleFactor = dpi / standardDpi;
                w = (int) Math.round(w * scaleFactor);
                h = (int) Math.round(h * scaleFactor);
                DpiRescaleStyleVisitor dpiVisitor = new DpiRescaleStyleVisitor(scaleFactor);
                dpiVisitor.visit(gt2Style);
                gt2Style = (Style) dpiVisitor.getCopy();
            }
            // apply UOM rescaling if we have a scale
            if (request.getScale() > 0) {
                double pixelsPerMeters = RendererUtilities.calculatePixelsPerMeterRatio(request.getScale(), request.getLegendOptions());
                UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
                rescaleVisitor.visit(gt2Style);
                gt2Style = (Style) rescaleVisitor.getCopy();
            }
            
            boolean strict = request.isStrict();
            
            final boolean transparent = request.isTransparent();
            RenderedImage titleImage=null;
            // if we have more than one layer, we put a title on top of each layer legend
            if(layers.size() > 1 && !forceTitlesOff) {
                titleImage=getLayerTitle(layer,  w, h, transparent, request);
            }
            
            final boolean buildRasterLegend = (!strict && layer == null && LegendUtils
                    .checkRasterSymbolizer(gt2Style)) || LegendUtils.checkGridLayer(layer);
            if (buildRasterLegend) {
                final RasterLayerLegendHelper rasterLegendHelper = new RasterLayerLegendHelper(request,gt2Style,ruleName);
                final BufferedImage image = rasterLegendHelper.getLegend();
                if(image != null) {
                    if(titleImage != null) {
                    layersImages.add(titleImage);
                }
                layersImages.add(image);
                }                
            } else {
                
                final Feature sampleFeature;
                if (layer == null) {
                    sampleFeature = createSampleFeature();
                } else {                    
                    sampleFeature = createSampleFeature(layer);
                }
                final FeatureTypeStyle[] ftStyles = gt2Style.featureTypeStyles().toArray(
                        new FeatureTypeStyle[0]);
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
                
                final NumberRange<Double> scaleRange = NumberRange.create(scaleDenominator,
                        scaleDenominator);
                final int ruleCount = applicableRules.length;
                
                /**
                 * A legend graphic is produced for each applicable rule. They're being held 
                 * here until the process is done and then painted on a "stack" like legend.
                 */
                final List<RenderedImage> legendsStack = new ArrayList<RenderedImage>(ruleCount);
                
                final SLDStyleFactory styleFactory = new SLDStyleFactory();
                
                double minimumSymbolSize = MINIMUM_SYMBOL_SIZE;
                // get minSymbolSize from LEGEND_OPTIONS, if defined
                if (request.getLegendOptions().get("minSymbolSize") instanceof String) {
                    String minSymbolSizeOpt = (String) request.getLegendOptions()
                            .get("minSymbolSize");
                    try {
                        minimumSymbolSize = Double.parseDouble(minSymbolSizeOpt);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid minSymbolSize value: should be a number");
                    }
                }
                // calculate the symbols rescaling factor necessary for them to be
                // drawn inside the icon box
                double symbolScale = calcSymbolScale(w, h, layer, sampleFeature,
                        applicableRules, minimumSymbolSize);
                
                for (int i = 0; i < ruleCount; i++) {
                    
                    final RenderedImage image = ImageUtils.createImage(w, h, (IndexColorModel) null,
                            transparent);
                    final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
                    final Graphics2D graphics = ImageUtils.prepareTransparency(transparent, LegendUtils.getBackgroundColor(request), image,
                            hintsMap);
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    Feature sample = getSampleFeatureForRule(layer,
                            sampleFeature, applicableRules[i]);
                    
                    FilterFactory ff = CommonFactoryFinder.getFilterFactory();
                    final Symbolizer[] symbolizers = applicableRules[i].getSymbolizers();
                    
                    for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                        Symbolizer symbolizer = symbolizers[sIdx];
                        
                        if (symbolizer instanceof RasterSymbolizer) {
                            throw new IllegalStateException(
                                    "It is not legal to have a RasterSymbolizer here");
                        } else {
                            // rescale symbols if needed
                            if (symbolScale > 1.0
                                    && symbolizer instanceof PointSymbolizer) {
                                PointSymbolizer pointSymbolizer = cloneSymbolizer(symbolizer);
                                if (pointSymbolizer.getGraphic() != null) {
                                    double size = getPointSymbolizerSize(sample,
                                            pointSymbolizer, Math.min(w, h) - 4);
                                    pointSymbolizer.getGraphic().setSize(
                                            ff.literal(size / symbolScale
                                                    + minimumSymbolSize));
    
                                    symbolizer = pointSymbolizer;
                                }
                            }
                            Style2D style2d = styleFactory.createStyle(sample,
                                    symbolizer, scaleRange);
                            LiteShape2 shape = getSampleShape(symbolizer, w, h);
    
                            if (style2d != null) {
                                shapePainter.paint(graphics, shape, style2d,
                                        scaleDenominator);
                            }
                        }
                    }
                    if(image != null && titleImage != null) {
                        layersImages.add(titleImage);
                        titleImage = null;
                    }
                    legendsStack.add(image);
                    graphics.dispose();
                }
                
                // JD: changed legend behavior, see GEOS-812
                // this.legendGraphic = scaleImage(mergeLegends(legendsStack), request);
                BufferedImage image = mergeLegends(legendsStack, applicableRules, request, 
                        forceLabelsOn, forceLabelsOff);
                if(image != null) {
                    layersImages.add(image);
                }
            }
            
        }
        // all legend graphics are merged if we have a layer group
        BufferedImage finalLegend = mergeLegends(layersImages,null,request, forceLabelsOn, forceLabelsOff);
        if(finalLegend == null) {
            throw new IllegalArgumentException("no legend passed");
        }
        return finalLegend;
    }

    /**
     * Clones the given (Point)Symbolizer.
     * 
     * @param symbolizer symbolizer to clone
     * @return cloned PointSymbolizer
     */
    private PointSymbolizer cloneSymbolizer(Symbolizer symbolizer) {
        DuplicatingStyleVisitor duplicator = new DuplicatingStyleVisitor();
        symbolizer.accept(duplicator);
        PointSymbolizer pointSymbolizer = (PointSymbolizer) duplicator
                .getCopy();
        return pointSymbolizer;
    }

    /**
     * Calculates a global rescaling factor for all the symbols
     * to be drawn in the given rules. This is to be sure all symbols
     * are drawn inside the given w x h box.
     * 
     * @param width horizontal constraint
     * @param height vertical constraint
     * @param featureType FeatureType to be used for size extraction in expressions
     *              (used to create a sample if feature is null)
     * @param feature Feature to be used for size extraction in expressions
     *              (if null a sample Feature will be created from featureType)
     * @param rules set of rules to scan for symbols
     * @param minimumSymbolSize lower constraint for the symbols size
     * @return
     */
    private double calcSymbolScale(int width, int height, FeatureType featureType,
            Feature feature, final Rule[] rules, double minimumSymbolsSize) {
        // check for max and min size in rendered symbols
        double minSize = Double.MAX_VALUE;
        double maxSize = 0.0;
    
        final int ruleCount = rules.length;
    
        for (int i = 0; i < ruleCount; i++) {
            Feature sample = getSampleFeatureForRule(featureType, feature, rules[i]);
            final Symbolizer[] symbolizers = rules[i].getSymbolizers();
            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];
                if (symbolizer instanceof PointSymbolizer) {
                    double size = getPointSymbolizerSize(sample,
                            (PointSymbolizer) symbolizer, Math.min(width, height));
                    if (size < minSize) {
                        minSize = size;
                    }
                    if (size > maxSize) {
                        maxSize = size;
                    }
                }
            }
        }
        if(minSize != maxSize) {
            return (maxSize - minSize + 1) / (Math.min(width, height) - minimumSymbolsSize);
        } else {
            return maxSize / (Math.min(width, height) - minimumSymbolsSize);
        }
    }

    /**
     * Gets a numeric value for the given PointSymbolizer
     * 
     * @param feature sample to be used for evals
     * @param pointSymbolizer symbolizer
     * @param defaultSize size to use is none can be taken from the symbolizer
     */
    private double getPointSymbolizerSize(Feature feature,
            PointSymbolizer pointSymbolizer, int defaultSize) {
        if (pointSymbolizer.getGraphic() != null) {
            Expression sizeExp = pointSymbolizer.getGraphic().getSize();
            if (sizeExp instanceof Literal) {
                Object size = sizeExp.evaluate(feature);
                if (size != null) {
                    if (size instanceof Double) {
                        return (Double) size;
                    }
                    try {
                        return Double.parseDouble(size.toString());
                    } catch (NumberFormatException e) {
                        return defaultSize;
                    }
    
                }
            }
        }
        return defaultSize;
    }

    /**
     * Returns a sample feature for the given rule, with the following criteria: -
     * if a sample is given in input is returned in output - if a sample is not
     * given in input, scan the rule symbolizers to find the one with the max
     * dimensionality, and return a sample for that dimensionality.
     * 
     * @param featureType featureType used to create a sample, if none is given as
     *        input
     * @param sample feature sample to be returned as is in output, if defined
     * @param rule rule containing symbolizers to scan for max dimensionality
     * @return
     */
    private Feature getSampleFeatureForRule(FeatureType featureType,
            Feature sample, final Rule rule) {
        Symbolizer[] symbolizers = rule.getSymbolizers();
        // if we don't have a sample as input, we need to create a sampleFeature
        // looking at the requested symbolizers (we chose the one with the max
        // dimensionality and create a congruent sample)
        if (sample == null) {
            int dimensionality = 1;
            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];
                if (LineSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 2;
                }
                if (PolygonSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 3;
                }
            }
            return createSampleFeature(featureType, dimensionality);
        } else {
            return sample;
        }
    }
    
    /**
     * Renders a title for a layer (to be put on top of the layer legend).
     * 
     * @param layer FeatureType representing the layer
     * @param w width for the image (hint)
     * @param h height for the image (hint)
     * @param transparent (should the image be transparent)
     * @param request GetLegendGraphicRequest being built
     * @return image with the title
     */
    private RenderedImage getLayerTitle(FeatureType layer, int w, int h, boolean transparent, 
            GetLegendGraphicRequest request) {
        // we choose a title with the following priority:
        //  - layer title
        //  - layer name
        String title=request.getTitle(layer.getName());
        if (title == null) {
            title=layer.getName().getLocalPart();
        }
        final BufferedImage image = ImageUtils.createImage(w, h, (IndexColorModel) null,
                transparent);
        return getRenderedLabel(image,title,request);
    }
   

    /**
     * Renders a label on the given image, using parameters from the request
     * for the rendering style.
     * 
     * @param image
     * @param label
     * @param request
     * @return
     */
    private BufferedImage getRenderedLabel(BufferedImage image, String label,
            GetLegendGraphicRequest request) {
        Font labelFont = LegendUtils.getLabelFont(request);
        boolean useAA = LegendUtils.isFontAntiAliasing(request);
                        
        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(labelFont);
        if(useAA) {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        return LegendUtils.renderLabel(label, graphics, request);
    }

    /**
     * Receives a list of <code>BufferedImages</code> and produces a new one which holds all the
     * images in <code>imageStack</code> one above the other, handling labels.
     * 
     * @param imageStack
     *            the list of BufferedImages, one for each applicable Rule
     * @param rules
     *            The applicable rules, one for each image in the stack (if not null it's used to
     *            compute labels)
     * @param request
     *            The request.
     * @param forceLabelsOn
     *            true for force labels on also with a single image.
     * @param forceLabelsOff
     *            true for force labels off also with more than one rule.
     * 
     * @return the stack image with all the images on the argument list.
     * 
     * @throws IllegalArgumentException
     *             if the list is empty
     */
    private BufferedImage mergeLegends(List<RenderedImage> imageStack, Rule[] rules,
            GetLegendGraphicRequest req, boolean forceLabelsOn, boolean forceLabelsOff) {

        Font labelFont = LegendUtils.getLabelFont(req);
        boolean useAA = LegendUtils.isFontAntiAliasing(req);

        

        if (imageStack.size() == 0) {
            return null;
        }

        final BufferedImage finalLegend;

        if (imageStack.size() == 1 && (!forceLabelsOn || rules == null)) {
            finalLegend = (BufferedImage) imageStack.get(0);
        } else {
            final int imgCount = imageStack.size();
            final String[] labels = new String[imgCount];

            BufferedImage img = ((BufferedImage) imageStack.get(0));

            int totalHeight = 0;
            int totalWidth = 0;
            int[] rowHeights = new int[imgCount];
            BufferedImage labelsGraphics[] = new BufferedImage[imgCount];
            for (int i = 0; i < imgCount; i++) {
                img = (BufferedImage) imageStack.get(i);

                if (forceLabelsOff || rules == null) {
                    totalWidth = (int) Math.ceil(Math.max(img.getWidth(), totalWidth));
                    rowHeights[i] = img.getHeight();
                    totalHeight += img.getHeight();
                } else {

                    Rule rule = rules[i];

                    // What's the label on this rule? We prefer to use
                    // the 'title' if it's available, but fall-back to 'name'
                    final Description description = rule.getDescription();
                    Locale locale = req.getLocale();
                    
                    if (description != null && description.getTitle() != null) {
                        final InternationalString title = description.getTitle();
                        if(locale != null) {
                        	labels[i] = title.toString(locale);
                        } else {
                        labels[i] = title.toString();
                        }
                    } else if (rule.getName() != null) {
                        labels[i] = rule.getName();
                    } else {
                        labels[i] = "";
                    }
                    
                    if (labels[i] != null && labels[i].length() > 0) {
                        final BufferedImage renderedLabel = getRenderedLabel(img,labels[i],req);
                        labelsGraphics[i] = renderedLabel;
                        final Rectangle2D bounds = new Rectangle2D.Double(0, 0,
                                renderedLabel.getWidth(), renderedLabel.getHeight());

                        totalWidth = (int) Math.ceil(Math.max(img.getWidth() + bounds.getWidth(),
                                totalWidth));
                        rowHeights[i] = (int) Math.ceil(Math.max(img.getHeight(),
                                bounds.getHeight()));
                    } else {
                        totalWidth = (int) Math.ceil(Math.max(img.getWidth(), totalWidth));
                        rowHeights[i] = (int) Math.ceil(img.getHeight());
                        labelsGraphics[i] = null;
                    }

                    totalHeight += rowHeights[i];

                }
            }

            // buffer the width a bit
            totalWidth += 2;

            final boolean transparent = req.isTransparent();
            final Color backgroundColor = LegendUtils.getBackgroundColor(req);
            final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
            // create the final image
            finalLegend = ImageUtils.createImage(totalWidth, totalHeight, (IndexColorModel) null,
                    transparent);
            Graphics2D finalGraphics = ImageUtils.prepareTransparency(transparent, backgroundColor,
                    finalLegend, hintsMap);

            int topOfRow = 0;

            for (int i = 0; i < imgCount; i++) {
                img = (BufferedImage) imageStack.get(i);

                // draw the image
                int y = topOfRow;

                if (img.getHeight() < rowHeights[i]) {
                    // move the image to the center of the row
                    y += (int) ((rowHeights[i] - img.getHeight()) / 2d);
                }

                finalGraphics.drawImage(img, 0, y, null);
                if (forceLabelsOff || rules == null) {
                    topOfRow += rowHeights[i];
                    continue;
                }

                finalGraphics.setFont(labelFont);

                if (useAA) {
                    finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                } else {
                    finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                }

                // draw the label
                if (labels[i] != null && labels[i].length() > 0) {
                    // first create the actual overall label image.
                    final BufferedImage renderedLabel = labelsGraphics[i];

                    y = topOfRow;

                    if (renderedLabel.getHeight() < rowHeights[i]) {
                        y += (int) ((rowHeights[i] - renderedLabel.getHeight()) / 2d);
                    }

                    finalGraphics.drawImage(renderedLabel, img.getWidth(), y, null);
                    // cleanup
                    renderedLabel.flush();
                    labelsGraphics[i] = null;
                }

                topOfRow += rowHeights[i];
            }

            finalGraphics.dispose();
        }
        return finalLegend;
    }
    
    

    /**
     * Returns a <code>java.awt.Shape</code> appropiate to render a legend graphic given the
     * symbolizer type and the legend dimensions.
     * 
     * @param symbolizer
     *            the Symbolizer for whose type a sample shape will be created
     * @param legendWidth
     *            the requested width, in output units, of the legend graphic
     * @param legendHeight
     *            the requested height, in output units, of the legend graphic
     * 
     * @return an appropiate Line2D, Rectangle2D or LiteShape(Point) for the symbolizer, wether it
     *         is a LineSymbolizer, a PolygonSymbolizer, or a Point ot Text Symbolizer
     * 
     * @throws IllegalArgumentException
     *             if an unknown symbolizer impl was passed in.
     */
    private LiteShape2 getSampleShape(Symbolizer symbolizer, int legendWidth, int legendHeight) {
        LiteShape2 sampleShape;
        final float hpad = (legendWidth * LegendUtils.hpaddingFactor);
        final float vpad = (legendHeight * LegendUtils.vpaddingFactor);

        if (symbolizer instanceof LineSymbolizer) {
            if (this.sampleLine == null) {
                Coordinate[] coords = { new Coordinate(hpad, legendHeight - vpad - 1),
                        new Coordinate(legendWidth - hpad - 1, vpad) };
                LineString geom = geomFac.createLineString(coords);

                try {
                    this.sampleLine = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleLine = null;
                }
            }

            sampleShape = this.sampleLine;
        } else if ((symbolizer instanceof PolygonSymbolizer)
                || (symbolizer instanceof RasterSymbolizer)) {
            if (this.sampleRect == null) {
                final float w = legendWidth - (2 * hpad) - 1;
                final float h = legendHeight - (2 * vpad) - 1;

                Coordinate[] coords = { new Coordinate(hpad, vpad), new Coordinate(hpad, vpad + h),
                        new Coordinate(hpad + w, vpad + h), new Coordinate(hpad + w, vpad),
                        new Coordinate(hpad, vpad) };
                LinearRing shell = geomFac.createLinearRing(coords);
                Polygon geom = geomFac.createPolygon(shell, null);

                try {
                    this.sampleRect = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleRect = null;
                }
            }

            sampleShape = this.sampleRect;
        } else if (symbolizer instanceof PointSymbolizer || symbolizer instanceof TextSymbolizer) {
            if (this.samplePoint == null) {
                Coordinate coord = new Coordinate(legendWidth / 2, legendHeight / 2);

                try {
                    this.samplePoint = new LiteShape2(geomFac.createPoint(coord), null, null, false);
                } catch (Exception e) {
                    this.samplePoint = null;
                }
            }

            sampleShape = this.samplePoint;
        } else {
            throw new IllegalArgumentException("Unknown symbolizer: " + symbolizer);
        }

        return sampleShape;
    }

    private SimpleFeature createSampleFeature() {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("Sample", "the_geom:Geometry");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return SimpleFeatureBuilder.template((SimpleFeatureType) type, null);
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the
     * rendering of the legend graphic, using the given dimensionality for the
     * geometry attribute.
     * 
     * @param schema the schema for which to create a sample Feature instance
     * @param dimensionality the geometry dimensionality required (ovverides the one
     *        defined in the schema) 1= points, 2= lines, 3= polygons
     * @return
     * @throws ServiceException
     */
    private Feature createSampleFeature(FeatureType schema, int dimensionality)
            throws ServiceException {
        if (schema instanceof SimpleFeatureType) {
            schema = cloneWithDimensionality(schema, dimensionality);
        }
    
        return createSampleFeature(schema);
    }

    /**
     * Clones the given schema, changing the geometry attribute to match the given
     * dimensionality.
     * 
     * @param schema schema to clone
     * @param dimensionality dimensionality for the geometry 1= points, 2= lines, 3=
     *        polygons
     * @return
     */
    private FeatureType cloneWithDimensionality(FeatureType schema,
            int dimensionality) {
        SimpleFeatureType simpleFt = (SimpleFeatureType) schema;
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        builder.setCRS(schema.getCoordinateReferenceSystem());
        for (AttributeDescriptor desc : simpleFt.getAttributeDescriptors()) {
            if (isMixedGeometry(desc)) {
                GeometryDescriptor geomDescriptor = (GeometryDescriptor) desc;
                GeometryType geomType = geomDescriptor.getType();
    
                Class<?> geometryClass = getGeometryForDimensionality(dimensionality);
    
                GeometryType gt = new GeometryTypeImpl(geomType.getName(),
                        geometryClass, geomType.getCoordinateReferenceSystem(),
                        geomType.isIdentified(), geomType.isAbstract(),
                        geomType.getRestrictions(), geomType.getSuper(),
                        geomType.getDescription());
    
                builder.add(new GeometryDescriptorImpl(gt,
                        geomDescriptor.getName(), geomDescriptor.getMinOccurs(),
                        geomDescriptor.getMaxOccurs(), geomDescriptor.isNillable(),
                        geomDescriptor.getDefaultValue()));
            } else {
                builder.add(desc);
            }
        }
        schema = builder.buildFeatureType();
        return schema;
    }
    
    /**
     * Creates a Geometry class for the given dimensionality.
     * 
     * @param dimensionality
     * @return
     */
    private Class<?> getGeometryForDimensionality(int dimensionality) {
        if (dimensionality == 1) {
            return Point.class;
        }
        if (dimensionality == 2) {
            return LineString.class;
        }
        return Polygon.class;
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the rendering of the
     * legend graphic.
     * 
     * @param schema
     *            the schema for which to create a sample Feature instance
     * 
     * @return
     * 
     * @throws ServiceException
     */
    private Feature createSampleFeature(FeatureType schema) throws ServiceException {
        Feature sampleFeature;
        try {            
            if (schema instanceof SimpleFeatureType) {
                if (hasMixedGeometry((SimpleFeatureType)schema)) {
                    // we can't create a sample for a generic Geometry type
                    sampleFeature = null;
                } else {                
                    sampleFeature = SimpleFeatureBuilder.template((SimpleFeatureType) schema, null);
                }
            } else {
                sampleFeature = DataUtilities.templateFeature(schema);
            }
        } catch (IllegalAttributeException e) {
            throw new ServiceException(e);
        }
        return sampleFeature;
    }

    /**
     * Checks if the given schema contains a GeometryDescriptor that has a generic
     * Geometry type.
     * 
     * @param schema
     * @return
     */
    private boolean hasMixedGeometry(SimpleFeatureType schema) {
        for (AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if(isMixedGeometry(attDesc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given AttributeDescriptor describes a generic Geometry.
     * 
     * @param attDesc
     */
    private boolean isMixedGeometry(AttributeDescriptor attDesc) {
        if (attDesc instanceof GeometryDescriptor
                && attDesc.getType().getBinding() == Geometry.class) {
            return true;
        }
        return false;
    }
    

}
