/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.awt.image.SampleModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;

import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.DecimationPolicy;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Interpolate;
import org.geotools.coverage.processing.operation.Resample;
import org.geotools.coverage.processing.operation.SelectSampleDimension;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.processing.Operation;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.wcs.WcsException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class WCSUtils {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(WCSUtils.class);
    
    public static final String ELEVATION = "ELEVATION";
    
    public final static Hints LENIENT_HINT = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
    
    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();
    
    private static final Operation CROP = PROCESSOR.getOperation("CoverageCrop");

    private final static SelectSampleDimension bandSelectFactory = new SelectSampleDimension();

    private final static Interpolate interpolateFactory = new Interpolate();

    private final static Resample resampleFactory = new Resample();

    private final static ParameterValueGroup bandSelectParams;

    private final static ParameterValueGroup interpolateParams;

    private final static ParameterValueGroup resampleParams;

    private final static Hints hints = new Hints();

    static {
        hints.add(LENIENT_HINT);
        // ///////////////////////////////////////////////////////////////////
        //
        // Static Processors
        //
        // ///////////////////////////////////////////////////////////////////
        final CoverageProcessor processor = CoverageProcessor.getInstance((LENIENT_HINT));
        bandSelectParams = processor.getOperation("SelectSampleDimension").getParameters();
        interpolateParams = processor.getOperation("Interpolate").getParameters();
        resampleParams = processor.getOperation("Resample").getParameters();        
    }

    /**
     * <strong>Reprojecting</strong><br>
     * The new grid geometry can have a different coordinate reference system than the underlying
     * grid geometry. For example, a grid coverage can be reprojected from a geodetic coordinate
     * reference system to Universal Transverse Mercator CRS.
     * 
     * 
     * @param coverage
     *            GridCoverage2D
     * @param sourceCRS
     *            CoordinateReferenceSystem
     * @param targetCRS
     *            CoordinateReferenceSystem
     * @return GridCoverage2D
     * @throws WcsException
     */
    public static GridCoverage2D resample(
    		final GridCoverage2D coverage,
            final CoordinateReferenceSystem sourceCRS, 
            final CoordinateReferenceSystem targetCRS,
            final GridGeometry2D gridGeometry,
            final Interpolation interpolation) throws WcsException {


        final ParameterValueGroup param = (ParameterValueGroup) resampleParams.clone();
        param.parameter("Source").setValue(coverage);
        param.parameter("CoordinateReferenceSystem").setValue(targetCRS);
        param.parameter("GridGeometry").setValue(gridGeometry);
        param.parameter("InterpolationType").setValue(interpolation);

        return (GridCoverage2D) resampleFactory.doOperation(param, hints);

    }
    
    /**
     * Crops the coverage to the specified bounds
     * 
     * @param coverage
     * @param bounds
     * @return
     */
    public static GridCoverage2D crop(
            final GridCoverage2D coverage,
            final Envelope bounds) {
        Polygon polygon = JTS.toGeometry(new ReferencedEnvelope(bounds));
        Geometry roi = polygon.getFactory().createMultiPolygon(new Polygon[] {polygon});

        // perform the crops
        final ParameterValueGroup param = CROP.getParameters();
        param.parameter("Source").setValue(coverage);
        param.parameter("Envelope").setValue(bounds);
        param.parameter("ROI").setValue(roi);

        return (GridCoverage2D) PROCESSOR.doOperation(param);
    }

    /**
     * <strong>Interpolating</strong><br>
     * Specifies the interpolation type to be used to interpolate values for points which fall
     * between grid cells. The default value is nearest neighbor. The new interpolation type
     * operates on all sample dimensions. Possible values for type are: {@code "NearestNeighbor"},
     * {@code "Bilinear"} and {@code "Bicubic"} (the {@code "Optimal"} interpolation type is
     * currently not supported).
     * 
     * @param coverage
     *            GridCoverage2D
     * @param interpolation
     *            Interpolation
     * @return GridCoverage2D
     * @throws WcsException
     */
    public static GridCoverage2D interpolate(
    		final GridCoverage2D coverage,
            final Interpolation interpolation) throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // INTERPOLATE
        //
        //
        // ///////////////////////////////////////////////////////////////////
        if (interpolation != null) {
            /* Operations.DEFAULT.interpolate(coverage, interpolation) */
            final ParameterValueGroup param = (ParameterValueGroup) interpolateParams.clone();
            param.parameter("Source").setValue(coverage);
            param.parameter("Type").setValue(interpolation);

            return (GridCoverage2D) interpolateFactory.doOperation(param, hints);
        }

        return coverage;
    }

    /**
     * <strong>Band Selecting</strong><br>
     * Chooses <var>N</var> {@linkplain org.geotools.coverage.GridSampleDimension sample dimensions}
     * from a grid coverage and copies their sample data to the destination grid coverage in the
     * order specified. The {@code "SampleDimensions"} parameter specifies the source
     * {@link org.geotools.coverage.GridSampleDimension} indices, and its size ({@code
     * SampleDimensions.length}) determines the number of sample dimensions of the destination grid
     * coverage. The destination coverage may have any number of sample dimensions, and a particular
     * sample dimension of the source coverage may be repeated in the destination coverage by
     * specifying it multiple times in the {@code "SampleDimensions"} parameter.
     * 
     * @param params
     *            Set
     * @param coverage
     *            GridCoverage
     * @return Coverage
     * @throws WcsException
     */
    public static Coverage bandSelect(final Map params, final GridCoverage coverage)
            throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // BAND SELECT
        //
        //
        // ///////////////////////////////////////////////////////////////////
        final int numDimensions = coverage.getNumSampleDimensions();
        final Map dims = new HashMap();
        final ArrayList selectedBands = new ArrayList();

        for (int d = 0; d < numDimensions; d++) {
            dims.put("band" + (d + 1), new Integer(d));
        }

        if ((params != null) && !params.isEmpty()) {
            for (Iterator p = params.keySet().iterator(); p.hasNext();) {
                final String param = (String) p.next();

                if (param.equalsIgnoreCase("BAND")) {
                    try {
                        final String values = (String) params.get(param);

                        if (values.indexOf("/") > 0) {
                            final String[] minMaxRes = values.split("/");
                            final int min = (int) Math.round(Double.parseDouble(minMaxRes[0]));
                            final int max = (int) Math.round(Double.parseDouble(minMaxRes[1]));
                            final double res = ((minMaxRes.length > 2) ? Double.parseDouble(minMaxRes[2]) : 0.0);

                            for (int v = min; v <= max; v++) {
                                final String key = param.toLowerCase() + v;

                                if (dims.containsKey(key)) {
                                    selectedBands.add(dims.get(key));
                                }
                            }
                        } else {
                            final String[] bands = values.split(",");

                            for (int v = 0; v < bands.length; v++) {
                                final String key = param.toLowerCase() + bands[v];

                                if (dims.containsKey(key)) {
                                    selectedBands.add(dims.get(key));
                                }
                            }

                            if (selectedBands.size() == 0) {
                                throw new Exception("WRONG PARAM VALUES.");
                            }
                        }
                    } catch (Exception e) {
                        throw new WcsException("Band parameters incorrectly specified: "
                                + e.getLocalizedMessage());
                    }
                }
            }
        }

        final int length = selectedBands.size();
        final int[] bands = new int[length];

        for (int b = 0; b < length; b++) {
            bands[b] = ((Integer) selectedBands.get(b)).intValue();
        }

        return bandSelect(coverage, bands);
    }

    public static Coverage bandSelect(final GridCoverage coverage, final int[] bands) {
        Coverage bandSelectedCoverage;

        if ((bands != null) && (bands.length > 0)) {
            /* Operations.DEFAULT.selectSampleDimension(coverage, bands) */
            final ParameterValueGroup param = (ParameterValueGroup) bandSelectParams.clone();
            param.parameter("Source").setValue(coverage);
            param.parameter("SampleDimensions").setValue(bands);
            // param.parameter("VisibleSampleDimension").setValue(bands);
            bandSelectedCoverage = bandSelectFactory.doOperation(param, hints);
        } else {
            bandSelectedCoverage = coverage;
        }

        return bandSelectedCoverage;
    }

    /**
     * Checks the coverage described by the specified geometry and sample model does not exceeds the output
     * WCS limits 
     * @param info
     * @param gridRange2D
     * @param sampleModel
     */
	public static void checkOutputLimits(WCSInfo info, GridEnvelope2D gridRange2D, SampleModel sampleModel) {
        // do we have to check a limit at all?
	    long limit = info.getMaxOutputMemory() * 1024;
        if(limit <= 0) {
            return;
        }
        
        // compute the coverage memory usage and compare with limit
        long actual = getCoverageSize(gridRange2D, sampleModel);
        if(actual > limit) {
            throw new WcsException("This request is trying to generate too much data, " +
                    "the limit is " + formatBytes(limit) + " but the actual amount of bytes to be " +
                            "written in the output is " + formatBytes(actual));
        }
    }

    /**
     * Checks the coverage read is below the input limits. Mind, at this point the reader might have
     * have subsampled the original image in some way so it is expected the coverage is actually
     * smaller than what computed but {@link #checkInputLimits(CoverageInfo, AbstractGridCoverage2DReader, GeneralEnvelope)},
     * however that method might have failed the computation due to lack of metadata (or wrong metadata)
     * so it's safe to double check the actual coverage wit this one.
     * Mind, this method might cause the coverage to be fully read in memory (if that is the case,
     * the actual WCS processing chain would result in the same behavior so this is not causing
     * any extra memory usage, just makes it happen sooner)
     * @param coverage
     */
    public static void checkInputLimits(WCSInfo info, GridCoverage2D coverage) {
        // do we have to check a limit at all?
        long limit = info.getMaxInputMemory() * 1024;
        if(limit <= 0) {
            return;
        }
        
        // compute the coverage memory usage and compare with limit
        long actual = getCoverageSize(coverage.getGridGeometry().getGridRange2D(), 
                coverage.getRenderedImage().getSampleModel());
        if(actual > limit) {
            throw new WcsException("This request is trying to read too much data, " +
                    "the limit is " + formatBytes(limit) + " but the actual amount of " +
                    		"bytes to be read is " + formatBytes(actual));
        }
    }
    
    /**
     * Computes the size of a grid coverage given its grid envelope and the target sample model
     * @param envelope
     * @param sm
     * @return
     */
    static long getCoverageSize(GridEnvelope2D envelope, SampleModel sm) {
        // === compute the coverage memory usage and compare with limit
        final long pixelsNumber = computePixelsNumber(envelope);
        
        
        long pixelSize = 0;
        final int numBands=sm.getNumBands();
        for (int i = 0; i < numBands; i++) {
            pixelSize += sm.getSampleSize(i);
        }
        return pixelsNumber * pixelSize / 8;
    }

    /**
     * Utility method to called to check the amount of data to be read does not exceed the WCS read limits.
     * This method has to jump through a few hoops to estimate the size of the data to be read without
     * having to actually read the coverage (which might trigger the loading of the full coverage in
     * memory) 
     * @param meta
     * @param reader
     * @param requestedEnvelope
     * @throws WcsException if the coverage size exceeds the configured limits
     */
    public static void checkInputLimits(WCSInfo info, CoverageInfo meta, 
            GridCoverage2DReader reader, GridGeometry2D gridGeometry) throws WcsException {
        // do we have to check a limit at all?
        long limit = info.getMaxInputMemory() * 1024;
        if(limit <= 0) {
            return;
        }
        
        // compute the actual amount of data read
        long actual = 0;
        try {
            // if necessary reproject back to the original CRS
            GeneralEnvelope requestedEnvelope = new GeneralEnvelope(gridGeometry.getEnvelope());
            final CoordinateReferenceSystem requestCRS = requestedEnvelope.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem nativeCRS = reader.getCoordinateReferenceSystem();
            if(!CRS.equalsIgnoreMetadata(requestCRS, nativeCRS)) {
                requestedEnvelope = CRS.transform(CRS.findMathTransform(requestCRS, nativeCRS, true), requestedEnvelope);
            }
            // intersect with the native envelope, we cannot read outside of it
            requestedEnvelope.intersect(reader.getOriginalEnvelope());
            
            // check if we are still reading anything
            if(!requestedEnvelope.isEmpty()) {
                MathTransform crsToGrid = meta.getGrid().getGridToCRS().inverse();
                GeneralEnvelope requestedGrid = CRS.transform(crsToGrid, requestedEnvelope);
                double[] spans = new double[requestedGrid.getDimension()];
                double[] resolutions = new double[requestedGrid.getDimension()];
                for (int i = 0; i < spans.length; i++) {
                    spans[i] = requestedGrid.getSpan(i);
                    resolutions[i] = requestedEnvelope.getSpan(i) / spans[i];
                }
                
                // adjust the spans based on the overview policy
                OverviewPolicy policy = info.getOverviewPolicy();
                double[] readResoutions = reader.getReadingResolutions(policy, resolutions);
                double[] baseResolutions = reader.getReadingResolutions(OverviewPolicy.IGNORE, resolutions);
                for (int i = 0; i < spans.length; i++) {
                    spans[i] *= readResoutions[i] / baseResolutions[i]; 
                }
                
                // compute how many pixels we're going to read
                long pixels = 1;
                for (int i = 0; i < requestedGrid.getDimension(); i++) {
                    pixels *= Math.ceil(requestedGrid.getSpan(i));
                }
                
                // compute the size of a pixel using the coverage metadata (the reader won't give
                // us any information about the bands)
                long pixelSize = 0;
                if(meta.getDimensions() != null) {
                    for (CoverageDimensionInfo dimension : meta.getDimensions()) {
                        int size = guessSizeFromRange(dimension.getRange());
                        if(size == 0) {
                            LOGGER.log(Level.INFO, "Failed to guess the size of dimension " 
                                    + dimension.getName() + ", skipping the pre-read check");
                            pixelSize = -1;
                            break;
                        }
                        pixelSize += size;
                    }
                }
                
                actual = pixels * pixelSize / 8;
            }
        } catch(Throwable t) {
            throw new WcsException("An error occurred while checking serving limits", t);
        }
        
        if(actual < 0) {
            // TODO: provide some more info about the request? It seems to be we'd have to
            // log again the full request... unless the request logger starts dumping the thread
            // id, in that case we can just refer to that and tell the admin to enable 
            // the request logger to debug these issues?
            LOGGER.log(Level.INFO, "Warning, we could not estimate the amount of bytes to be " +
                    "read from the coverage source for the current request");
        }
        
        if(actual > limit) {
            throw new WcsException("This request is trying to read too much data, " +
                    "the limit is " + formatBytes(limit) + " but the actual amount of bytes " +
                    		"to be read is " + formatBytes(actual));
        }
    }

    /**
     * Guesses the size of the sample able to contain the range fully
     * @param range
     * @return
     */
    static int guessSizeFromRange(NumberRange range) {
        double min = range.getMinimum();
        double max = range.getMaximum();
        double diff = max - min;
        
        if(diff <= ((int) Byte.MAX_VALUE - (int) Byte.MIN_VALUE)) {
            return 8;
        } else if(diff <= ((int) Short.MAX_VALUE - (int) Short.MIN_VALUE)) {
            return 16;
        } else if(diff <= ((double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE)) {
            return 32;
        } else if(diff <= ((double) Float.MAX_VALUE - (double) Float.MIN_VALUE)) {
            return 32;
        } else {
            return 64;
        }
    }
    
    /**
     * Utility function to format a byte amount into a human readable string
     * @param bytes
     * @return
     */
    static String formatBytes(long bytes) {
        if(bytes < 1024) {
            return bytes + "B";
        } else if(bytes < 1024 * 1024) {
            return new DecimalFormat("#.##").format(bytes / 1024.0) + "KB";
        } else {
            return new DecimalFormat("#.##").format(bytes / 1024.0 / 1024.0) + "MB";
        }
    }

    /**
     * Returns the reader hints based on the current WCS configuration
     * @param wcs
     * @return
     */
    public static Hints getReaderHints(WCSInfo wcs) {
        Hints hints = new Hints();
        hints.add(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        if (wcs.getOverviewPolicy() == null) {
            hints.add(new Hints(Hints.OVERVIEW_POLICY, OverviewPolicy.IGNORE));
        } else {
            hints.add(new Hints(Hints.OVERVIEW_POLICY, wcs.getOverviewPolicy()));
        }
        hints.put(Hints.DECIMATION_POLICY, wcs.isSubsamplingEnabled() ? DecimationPolicy.ALLOW
                : DecimationPolicy.DISALLOW);
        return hints;
    }
    
    /**
     * Returns an eventual filter included among the parsed kvp map of the current
     * request. Will work for CQL_FILTER, FILTER and FEATURE_ID
     * @return
     */
    public static Filter getRequestFilter() {
        Request request = Dispatcher.REQUEST.get();
        if(request == null) {
            return null;
        }
        Object filter = request.getKvp().get("FILTER");
        if(!(filter instanceof Filter)) {
            filter = request.getKvp().get("CQL_FILTER");
            if(filter instanceof List) {
                List list = (List) filter;
                if(list.size() > 0) {
                    filter = list.get(0);
                }
            }
        }
        if(!(filter instanceof Filter)) {
            filter = request.getKvp().get("FEATURE_ID");
        }
        
        if(filter instanceof Filter) {
            return (Filter) filter;
        } else {
            return null;
        }
    }
    /**
     * Computes the number of pixels for this {@link GridEnvelope2D}.
     * 
     * @param rasterEnvelope the {@link GridEnvelope2D} to compute the number of pixels for
     * @return the number of pixels for the provided {@link GridEnvelope2D}
     */
    private static long computePixelsNumber(GridEnvelope2D rasterEnvelope){
        // pixels
        long pixelsNumber=1;
        final int dimensions= rasterEnvelope.getDimension();
        for(int i = 0; i <dimensions; i++) {
            pixelsNumber *= rasterEnvelope.getSpan(i);
        }
        return pixelsNumber;
    }
}
