/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.factory.GeoTools;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
/**
 * The class that does the real work of checking if we are exceeeding
 * the download limits for vector data.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
class VectorDownload {
    
    private static final Logger LOGGER = Logging.getLogger(VectorDownload.class);

    /** The estimator. */
    private DownloadEstimatorProcess estimator;
    
    /**
     * Constructor 
     * 
     * @param estimator the parent {@link DownloadEstimatorProcess} that contains the download limits to be enforced.
     * 
     */
    public VectorDownload(DownloadEstimatorProcess estimator) {
        this.estimator = estimator;
    }

    /**
     * Extract vector data to a file, given the provided mime-type.
     * 
     * @param resourceInfo the {@link FeatureTypeInfo} to download from
     * @param mimeType the mme-type for the requested output format
     * @param roi the {@link Geometry} for the clip/intersection
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @param targetCRS the reproject {@link CoordinateReferenceSystem} 
     * @param progressListener
     * @param collector collector for temp files to delete
     * @return a file, given the provided mime-type.
     * @throws Exception
     */
    public File execute(FeatureTypeInfo resourceInfo, String mimeType, Geometry roi, boolean clip,
            Filter filter, CoordinateReferenceSystem targetCRS,
            final ProgressListener progressListener, TempFilesCollector collector) throws Exception {

        // prepare native CRS
        CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(resourceInfo);
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


        //
        // STEP 1 - Read and Filter
        //

        // access feature source and collection of features
        final SimpleFeatureSource featureSource = (SimpleFeatureSource) resourceInfo
                .getFeatureSource(null, GeoTools.getDefaultHints()); 

        // basic filter preparation
        Filter ra = Filter.INCLUDE;
        if (filter != null) {
            ra = filter;
        }
        // and with the ROI if we have one
        SimpleFeatureCollection originalFeatures;
        if (roiManager != null) {
            final String dataGeomName = featureSource.getSchema().getGeometryDescriptor()
                    .getLocalName();
            final Intersects intersectionFilter = FeatureUtilities.DEFAULT_FILTER_FACTORY
                    .intersects(FeatureUtilities.DEFAULT_FILTER_FACTORY.property(dataGeomName),
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.literal(roiManager.getSafeRoiInNativeCRS()));
            ra = FeatureUtilities.DEFAULT_FILTER_FACTORY.and(ra, intersectionFilter);
        }

        // simpplify filter
        ra = (Filter) ra.accept(new SimplifyingFilterVisitor(), null);
        // read
        originalFeatures = featureSource.getFeatures(ra);
        DownloadUtilities.checkIsEmptyFeatureCollection(originalFeatures);


        //
        // STEP 2 - Reproject feature collection
        //
        // do we need to reproject?
        SimpleFeatureCollection reprojectedFeatures;
        if (targetCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, targetCRS)) {
            roiManager.useTargetCRS(targetCRS);
            // testing reprojection...
            final MathTransform targetTX = CRS.findMathTransform(nativeCRS, targetCRS,true);
            if (!targetTX.isIdentity()) {
                // avoid doing the transform if this is the identity
                reprojectedFeatures = new ReprojectingFeatureCollection(originalFeatures, targetCRS);
            } else {
                reprojectedFeatures = originalFeatures;
                DownloadUtilities.checkIsEmptyFeatureCollection(reprojectedFeatures);
            }
        } else {
            reprojectedFeatures = originalFeatures;
            roiManager.useTargetCRS(nativeCRS);
        }
        
        //
        // STEP 3 - Clip in targetCRS
        //
        SimpleFeatureCollection clippedFeatures;
        if (clip && roi != null) {
            final ClipProcess clipProcess = new ClipProcess();// TODO avoid unnecessary creation
            clippedFeatures = clipProcess.execute(reprojectedFeatures, roiManager.getSafeRoiInTargetCRS(), true);

            // checks
            DownloadUtilities.checkIsEmptyFeatureCollection(clippedFeatures);
        } else {
            clippedFeatures = reprojectedFeatures;
        }

        //
        // STEP 4 - Write down respecting limits in bytes
        //
        // writing the output, making sure it is a zip
        return writeVectorOutput(clippedFeatures, resourceInfo.getName(),
                mimeType,collector);

    }

    /**
     * Write vector output with the provided PPIO. It returns the {@link File} it writes to.
     * 
     * @param features
     * @param name
     * @param mimeType
     * @param collector 
     * @return
     * @throws Exception
     */
    private File writeVectorOutput(final SimpleFeatureCollection features, final String name, final String mimeType, TempFilesCollector collector)
            throws Exception {

        // Search a proper PPIO
        ProcessParameterIO ppio_ = DownloadUtilities.find(new Parameter<SimpleFeatureCollection>(
                "fakeParam", SimpleFeatureCollection.class), null, mimeType, false);
        if (ppio_ == null) {
            throw new ProcessException("Don't know how to encode in mime type " + mimeType);
        } else if (!(ppio_ instanceof ComplexPPIO)) {
            throw new ProcessException("Invalid PPIO found " + ppio_.getIdentifer());
        }

        // limits
        long limit = DownloadEstimatorProcess.NO_LIMIT;
        if (estimator.getHardOutputLimit() > 0) {
            limit = estimator.getHardOutputLimit();
        }

        //
        // Get fileName
        //
        String extension = "";
        if (ppio_ instanceof ComplexPPIO) {
            extension = "." + ((ComplexPPIO) ppio_).getFileExtension();
        }

        // create output file
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource temp = loader.get("temp");
        final File output = File.createTempFile(name, extension, temp.dir());
        output.deleteOnExit();
        collector.addFile(output);//schedule for clean up

        // write checking limits
        OutputStream os = null;
        try {

            // create OutputStream that checks limits
            final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                    new FileOutputStream(output));
            if (limit > DownloadEstimatorProcess.NO_LIMIT) {
                os = new LimitedOutputStream(bufferedOutputStream, limit) {

                    @Override
                    protected void raiseError(long pSizeMax, long pCount) throws IOException {
                        IOException ioe= new IOException("Download Exceeded the maximum HARD allowed size!");
                        throw ioe;
                    }

                };

            } else {
                os = bufferedOutputStream;
            }

            // write with PPIO
            if (ppio_ instanceof ComplexPPIO) {
                ((ComplexPPIO) ppio_).encode(features, os);
            }
            os.flush();
        } finally {
            if (os != null) {
                IOUtils.closeQuietly(os);
            }
        }

        // return
        return output;

    }
}