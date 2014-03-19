/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * This class check whether or not the provided download request goes beyond the provided
 * limits for raster data or not.
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
class RasterEstimator {

    private static final Logger LOGGER = Logging.getLogger(RasterEstimator.class);

    /** The parent process. */
    private DownloadEstimatorProcess estimator;

    /**
     * Constructor 
     * 
     * @param estimator the parent {@link DownloadEstimatorProcess} that contains the download limits to be enforced.
     * 
     */
    public RasterEstimator(DownloadEstimatorProcess estimator) {
        this.estimator = estimator;
        if(estimator==null){
            throw new NullPointerException("The provided DownloadEstimatorProcess is null!");
        }
    }

    /**
     * Check the download limits for raster data.
     * 
     * @param coverage the {@link CoverageInfo} to estimate the download limits
     * @param roi the {@link Geometry} for the clip/intersection
     * @param targetCRS the reproject {@link CoordinateReferenceSystem} (useless for the moment)
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @return
     */
    public boolean execute(final ProgressListener progressListener, CoverageInfo coverageInfo,
            Geometry roi, CoordinateReferenceSystem targetCRS, boolean clip, Filter filter)
            throws Exception {

        //
        // Do we need to do anything?
        //
        final long readLimits=estimator.getReadLimits(); 
        if (readLimits <= 0) {
            LOGGER.fine("No raster read limits, moving on....");
            return true;
        }
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Raster read limits: "+readLimits);
        }

        //
        // ---> READ FROM NATIVE RESOLUTION <--
        //
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Checking download limits for raster request");
        }
        CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(coverageInfo);
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Native CRS is "+nativeCRS.toWKT());
        }
        
        //
        // STEP 0 - Push ROI back to native CRS (if ROI is provided)
        //
        ROIManager roiManager= null;
        if (roi != null) {
            CoordinateReferenceSystem roiCRS = (CoordinateReferenceSystem) roi.getUserData();
            roiManager=new ROIManager(roi, roiCRS);
            // set use nativeCRS
            roiManager.useNativeCRS(nativeCRS);
        }
        
        // get a reader for this CoverageInfo
        final AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);

        // read GridGeometry preparation
        final double areaRead;
        if (roi != null) {
            final Geometry safeRoiInNativeCRS = roiManager.getSafeRoiInNativeCRS();
            Geometry roiInNativeCRS_ = safeRoiInNativeCRS.intersection(FeatureUtilities.getPolygon(
                    reader.getOriginalEnvelope(), new GeometryFactory(new PrecisionModel(
                            PrecisionModel.FLOATING))));
            if (roiInNativeCRS_.isEmpty()) {
                return true; // EMPTY Intersection
            }
            // world to grid transform
            final AffineTransform2D w2G = (AffineTransform2D) reader.getOriginalGridToWorld(
                    PixelInCell.CELL_CORNER).inverse();
            final Geometry rasterGeometry = JTS.transform(roiInNativeCRS_, w2G);
            
            // try to make an estimate of the area we need to read 
            // NOTE I use the envelope since in the end I can only pass down
            // a rectangular source region to the ImageIO-Ext reader, but in the end I am only going 
            // to read the tile I will need during processing as in this case I am going to perform
            // deferred reads
            areaRead = rasterGeometry.getEnvelope().getArea();         
            // TODO investigate on improved precision taking into account tiling on raster geometry

        } else {
            // No ROI, we are trying to read the entire coverage
            final Rectangle2D originalGridRange = (GridEnvelope2D) reader.getOriginalGridRange();
            areaRead = originalGridRange.getWidth() * originalGridRange.getHeight();

        }
        // checks on the area we want to download
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Area to read in pixels: "+areaRead);
        }        
        if (areaRead > readLimits) {
            return false;
        }
        return true;

    }
}