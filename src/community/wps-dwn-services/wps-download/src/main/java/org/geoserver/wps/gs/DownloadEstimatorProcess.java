/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The Class DownloadEstimatorProcess.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
@DescribeProcess(title = "Enterprise Download Process", description = "Downloads Layer Stream and provides a ZIP.")
public class DownloadEstimatorProcess implements GSProcess {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(DownloadEstimatorProcess.class);

    /** The Constant DEFAULT_MAX_FEATURES. */
    public static final long DEFAULT_MAX_FEATURES = 1000000;

    /** Value used to indicate no limits */
    public static final long NO_LIMIT = 0;

    /** The max features. */
    private final long maxFeatures;

    /** The read limits. */
    private final long readLimits;

    /** The write limits. */
    private final long writeLimits;

    /** The hard output limit. */
    private final long hardOutputLimit;

    /** The catalog. */
    private final Catalog catalog;

    /**
     * @param readLimits
     * @param writeLimits
     * @param hardOutputLimit
     * @param geoserver
     */
    public DownloadEstimatorProcess(long readLimits, long writeLimits, long maxFeatures,
            long hardOutputLimit, GeoServer geoserver) {
        this.readLimits = readLimits;
        this.maxFeatures = maxFeatures;
        this.writeLimits = writeLimits;
        this.hardOutputLimit = hardOutputLimit;
        this.catalog = geoserver.getCatalog();
    }

    /**
     * Execute.
     * 
     * @param layerName the layer name
     * @param filter the filter
     * @param email the email
     * @param outputFormat the output format
     * @param targetCRS the target crs
     * @param roiCRS the roi crs
     * @param roi the roi
     * @param clip the crop to geometry
     * @param progressListener the progress listener
     * @return the boolean
     * @throws Exception
     */
    @DescribeResult(name = "result", description = "Download Limits are respected or not!")
    public Boolean execute(
            @DescribeParameter(name = "layerName", min = 1, description = "Original layer to download") String layerName,
            @DescribeParameter(name = "filter", min = 0, description = "Optional Vectorial Filter") Filter filter,
            @DescribeParameter(name = "targetCRS", min = 0, description = "Target CRS") CoordinateReferenceSystem targetCRS,
            @DescribeParameter(name = "RoiCRS", min = 0, description = "Region Of Interest CRS") CoordinateReferenceSystem roiCRS,
            @DescribeParameter(name = "ROI", min = 0, description = "Region Of Interest") Geometry roi,
            @DescribeParameter(name = "cropToROI", min = 0, description = "Crop to ROI") Boolean clip,
            ProgressListener progressListener) throws Exception {

        //
        // initial checks on mandatory params
        //
        // layer name
        if (layerName == null || layerName.length() <= 0) {
            throw new IllegalArgumentException("Empty or null layerName provided!");
        }
        if (clip == null) {
            clip = false;
        }
        if (roi != null) {
            DownloadUtilities.checkPolygonROI(roi);
            if (roiCRS == null) {
                throw new IllegalArgumentException("ROI without a CRS is not usable!");
            }
            roi.setUserData(roiCRS);
        }

        //
        // Move on with the real code
        //
        // cheking for the rsources on the GeoServer catalog
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            // could not find any layer ... abruptly interrupt the process
            throw new IllegalArgumentException("Unable to locate layer: " + layerName);

        }
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo == null) {
            // could not find any data store associated to the specified layer ... abruptly interrupt the process
            throw new IllegalArgumentException("Unable to locate ResourceInfo for layer:"
                    + layerName);

        }

        // ////
        // 1. DataStore -> look for vectorial data download
        // 2. CoverageStore -> look for raster data download
        // ////
        if (resourceInfo instanceof FeatureTypeInfo) {
            final FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) resourceInfo;

            return new VectorEstimator(this).execute(featureTypeInfo, roi, clip, filter, targetCRS,
                    progressListener);

        } else if (resourceInfo instanceof CoverageInfo) {
            final CoverageInfo coverage = (CoverageInfo) resourceInfo;
            return new RasterEstimator(this).execute(progressListener, coverage, roi, targetCRS,
                    clip, filter);
        }

        // the requeste layer is neither a featuretype nor a coverage --> error
        final ProcessException ex = new ProcessException(
                "Could not complete the Download Process: target resource is of Illegal type --> "
                        + resourceInfo != null ? resourceInfo.getClass().getCanonicalName()
                        : "null");
        if (progressListener != null) {
            progressListener.exceptionOccurred(ex);
        }
        throw ex;

    }

    /**
     * Gets the max features.
     * 
     * @return the maxFeatures
     */
    public long getMaxFeatures() {
        return maxFeatures;
    }

    /**
     * Gets the read limits.
     * 
     * @return the readLimits
     */
    public long getReadLimits() {
        return readLimits;
    }

    /**
     * Gets the write limits.
     * 
     * @return the writeLimits
     */
    public long getWriteLimits() {
        return writeLimits;
    }

    /**
     * Gets the hard output limit.
     * 
     * @return the hardOutputLimit
     */
    public long getHardOutputLimit() {
        return hardOutputLimit;
    }
}