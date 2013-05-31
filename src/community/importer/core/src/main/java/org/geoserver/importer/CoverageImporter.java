/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: CoverageImporter.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.importer;

import static org.geoserver.importer.LayerImportStatus.DEFAULTED_SRS;
import static org.geoserver.importer.LayerImportStatus.DUPLICATE;
import static org.geoserver.importer.LayerImportStatus.MISSING_BBOX;
import static org.geoserver.importer.LayerImportStatus.MISSING_NATIVE_CRS;
import static org.geoserver.importer.LayerImportStatus.NO_SRS_MATCH;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.DataUtilities;
import org.geotools.util.DefaultProgressListener;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <p>
 * Tries to import all or some of the GeoTIFFs in a directory, provides the ability to observe the process and to stop it prematurely.
 * </p>
 * <p>
 * It is advised to run it into its own thread
 * </p>
 * 
 * @author Luca Morandini lmorandini@ieee.org
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 * 
 * 
 */
public class CoverageImporter extends Importer {

    /**
     * Builds layer name (the whole part of file name bar the extension)
     * 
     */
    static String buildLayerName(String fileName) {
        int i = fileName.lastIndexOf(".");

        return (i < 1) ? fileName : fileName.substring(0, i);
    }

    private CoverageImportConfiguration config;

    private List<CoverageTransformer> transformers;

    /**
     * Constructor
     * 
     * @param id Id of importer
     * 
     * @param tm Object that manages import thrads
     * 
     * @param wsName workpace to be used on import
     * @param imageFile the directory contianing the raster data to import or a single image file to import
     * @param outputDirectory Directory containing output data
     * @param defaultSRS The default SRS to use when data have none
     * @param resources The list of resources to import. Use {@code null} to import all available ones
     * @param catalog The GeoServer catalog
     * @param workspaceNew Marks the workspace as newly created and ready for rollback
     * @param storeNew Marks the store as newly created and ready for rollback
     * @param copy Copies image to GeoServer Data Dir
     * @param tile Uses tile on generated images
     * @param overview Adds overviews to generated images
     * @param compressiontype Type of image compression
     * @param compressionratio Ratio of compression
     * @param tilewidth Width of tiles in pixels
     * @param tileheight height of tiles in pixels
     * @param retainTile Retain tiles if existing
     * @param downsamplestep Step of reduciton for overviews
     * @param subsamplealgorithm Algorithm used for subsampling in overviews
     * @param nOverviews N. of overviews
     * @param subsamplealgorithm Sub sampling algorithm
     * @param retainOverviews Retain overviews if existing
     * @param extoverview Use external overviews
     * @param dataDirectory Data directory of GeoServer instance
     */
    public CoverageImporter(String id, ImporterThreadManager tm, String defaultSRS,
            Set<Name> resources, Catalog catalog, boolean workspaceNew, boolean storeNew,
            CoverageImportConfiguration config) {

        super(id, tm, defaultSRS, resources, catalog, workspaceNew, storeNew);
        this.config = config;
        this.summary = new ImportSummary(id, config.getImageFile().getName(), workspaceNew,
                storeNew);

        // TODO: add new possible transfomers here
        CoverageTransformer gtt = new GeoToolsCoverageTransformer(defaultSRS);
        transformers = Arrays.asList(gtt);
    }

    public String getProject() {
        return this.config.getImageFile().getName();
    }

    /**
     * Returns an image file composed by the directory (or single file) stored in imageFile and the name of the single resource
     * 
     * @param resourceName name of the resourc to be retrieved
     */
    protected File buildImageFile(String resourceName) {
        if (this.config.getImageFile().isDirectory()) {
            return (new File(this.config.getImageFile().getAbsolutePath() + File.separator
                    + resourceName));
        } else {
            return (new File(this.config.getImageFile().getAbsolutePath()));
        }
    }

    /**
     * Returns an image file in the GeoServer data directory composed by the output dir and the name of the single resource
     * 
     * @param resourceName name of the resourc to be retrieved
     * @throws IOException
     */
    protected File buildOutputImageFile(String resourceName) throws IOException {
        return new File(this.config.getOutputDirectory(), resourceName);
    }

    /**
     * Executes the import
     * 
     * @see org.geoserver.importer.Importer#run()
     */
    public void run() {
        // the importer is running in a background thread, make it use the same authentication
        // as the submitter
        if(authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        LayerInfo covLayer = null;

        //
        // Save the path to the output directory for later showing.
        //
        String outputDirectoryPath;
        try {
            outputDirectoryPath = config.getOutputDirectory().getCanonicalPath();
        } catch (IOException e1) {
            outputDirectoryPath = config.getOutputDirectory().getAbsolutePath();
        }

        // Initializes summary of import and workspace data
        summary.setTotalLayers(this.resources.size());

        final WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(this.config.getWorkspace());
        if (workspaceInfo == null) {
            LOGGER.log(Level.FINE, "Cannot find workspace " + this.config.getWorkspace(),
                    new Exception());
            summary.completeLayer(null, null,
                    "Cannot find workspace " + this.config.getWorkspace(), LayerImportStatus.OTHER);

            return;
        }

        try {
            // For every file to be imported
            for (final Name resource : this.resources) {
                // let's move on
                LayerImportStatus status = LayerImportStatus.SUCCESS;

                // Add current file's information to summary
                summary.newLayer(resource.getLocalPart());

                // Read input file
                File currentFile = this.buildImageFile(resource.getLocalPart());

                // find the transformer that can deal with this file
                CoverageTransformer transformer = null;
                for (CoverageTransformer ct : transformers) {
                    if (ct.accepts(currentFile)) {
                        transformer = ct;
                        break;
                    }
                }
                if (transformer == null) {
                    // FIXME: message should be i18n
                    String message = "Input file is not a supported raster file:"
                            + currentFile.getCanonicalPath();
                    LOGGER.log(Level.INFO, message);
                    summary.completeLayer(resource.getLocalPart(), null, message,
                            LayerImportStatus.UNSUPPORTED_FORMAT);

                    continue;
                }

                try {
                    // do the transormations if needed
                    layerProgress(resource,
                            "Preparing to process file:" + currentFile.getCanonicalPath(), 0.0);
                    File file = copyAndTransform(resource, transformer, currentFile,
                            outputDirectoryPath);
                    layerProgress(resource, "Publishing layer in GeoServer", 90.0);

                    // Builds coverage store
                    CatalogBuilder builder = new CatalogBuilder(this.catalog);
                    CoverageStoreInfo covstoreInfo = builder.buildCoverageStore(resource
                            .getLocalPart());
                    covstoreInfo.setType(ImportUtilities.GEOTIFF_FORMAT.getName());
                    covstoreInfo.setURL(DataUtilities.fileToURL(file).toString());
                    covstoreInfo.setWorkspace(workspaceInfo);
                    covstoreInfo.setEnabled(true);
                    builder.setStore(covstoreInfo);

                    // Creates coverage and layer
                    CoverageInfo covInfo = builder.buildCoverage();
                    covInfo.setName(buildLayerName(resource.getLocalPart()));
                    builder.initCoverage(covInfo);
                    covLayer = builder.buildLayer(covInfo);

                    // If the layer exists, drop the old one from the catalog
                    if (catalog.getLayerByName(this.config.getWorkspace() + ":"
                            + covLayer.getName()) != null) {
                        catalog.remove(covLayer);
                    }

                    // Sets SRS
                    if ((covLayer.getResource().getSRS() == null)
                            && (covLayer.getResource().getNativeCRS() != null)
                            && (defaultSRS != null)) {
                        covLayer.getResource().setSRS(defaultSRS);
                        covLayer.getResource().setProjectionPolicy(
                                ProjectionPolicy.REPROJECT_TO_DECLARED);
                        status = DEFAULTED_SRS;
                    }

                    // Handles common error conditions

                    // Checks if a store with same name already exists: if it
                    // does and it is a coverage one, deletes it for later re-use, if it does
                    // but it is a vector one, raises an error
                    StoreInfo preExistingCovStore = this.catalog.getStoreByName(
                            this.config.getWorkspace(), resource.getLocalPart(), StoreInfo.class);
                    if (preExistingCovStore != null) {
                        if (preExistingCovStore instanceof CoverageStoreInfo) {
                            CascadeDeleteVisitor eraser = new CascadeDeleteVisitor(this.catalog);
                            eraser.visit((CoverageStoreInfo) preExistingCovStore);
                            LOGGER.log(Level.FINE, "Store " + preExistingCovStore.getName()
                                    + " already exists, it has been reused");
                        } else {
                            // FIXME: message should be i18n
                            summary.completeLayer(resource.getLocalPart(), covLayer,
                                    "Error, duplicate layer", DUPLICATE);

                            continue;
                        }
                    }

                    if (catalog.getCoverageByName(this.config.getWorkspace() + ":"
                            + covLayer.getName()) != null) {
                        status = DUPLICATE;
                    } else if ((covLayer.getResource().getSRS() == null) && (defaultSRS == null)) {
                        if (covLayer.getResource().getNativeCRS() == null) {
                            status = MISSING_NATIVE_CRS;
                        } else {
                            status = NO_SRS_MATCH;
                        }
                    } else if (covLayer.getResource().getLatLonBoundingBox() == null) {
                        status = MISSING_BBOX;
                    } else {
                        // Tries to save the layer
                        catalog.add(covstoreInfo);
                        catalog.add(covInfo);
                        try {
                            catalog.add(covLayer);
                            // get a proxy that we can modify
                            covLayer = catalog.getLayer(covLayer.getId());
                        } catch (Exception e) {
                            // will be caught by the external try/catch, here we
                            // just try to undo
                            // the feature type saving (transactions, where are
                            // you...)
                            catalog.remove(covLayer);
                            throw e;
                        }
                    }

                    // FIXME: message should be i18n
                    summary.completeLayer(resource.getLocalPart(), covLayer, "Done!", status);
                } catch (CancelException e) {
                    summary.end(true);
                    this.clearTask();
                    return;
                } catch (Exception e) {
                    summary.completeLayer(resource.getLocalPart(), covLayer,
                            "Error:" + e.getLocalizedMessage(), e);
                }

                // abort requested?
                if (cancelled) {
                    summary.end(true);
                    this.clearTask();

                    return;
                }
            }

            summary.end(false);
            this.clearTask();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Import process failed", e);
            summary.end(e);
            this.clearTask();
        }
    }

    private File copyAndTransform(final Name resource, CoverageTransformer transformer,
            File inputFile, final String outputDirectoryPath) throws IOException, CancelException {
        // for the moment manipulate the file only if copy is enabled
        CompressionConfiguration compression = this.config.getCompression();
        TilingConfiguration tiling = this.config.getTiling();
        OverviewConfiguration overviews = this.config.getOverview();

        // were are we going to write the output? Do we have to copy or we need to setup
        // a temporary file that we can work on?
        File outputFile = null;
        boolean temporaryFile = false;
        boolean transformationRequired = transformationRequired(transformer, inputFile);
        boolean overviewsEmbeddingRequired = overviewEmbeddingRequired(transformer, inputFile, transformationRequired);
        if (config.isCopy()) {
            outputFile = this.buildOutputImageFile(resource.getLocalPart());
        } else if (!config.isCopy() && transformationRequired) {
            outputFile = File.createTempFile(resource.getLocalPart(), ".tif");
            temporaryFile = true;
        }
        if (outputFile != null && outputFile.exists() && this.config.isCopy()) {
            try {
                outputFile.delete();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Cannot remove file " + outputFile.getCanonicalPath(), e);
            }
        }

        // copy it over in case we don't need to do retiling or re-compression
        if (config.isCopy() && !transformationRequired) {
            copyFiles(resource, inputFile, outputFile);
        } else {
            // setup how to split the percentages between compress/retile and
            // overview computation
            final double overviewQuota, transformQuota;
            if (compression.isEnabled() || tiling.isEnabled()) {
                if (overviews.isEnabled()) {
                    overviewQuota = 20;
                    transformQuota = 70;
                } else {
                    overviewQuota = 0;
                    transformQuota = 90;
                }
            } else {
                transformQuota = 0;
                overviewQuota = 90;
            }

            try {
                // even if we just have to embed overviews we cannot know if the original had some
                // or not, if they did the overview embedded will produce junk so we force a copy anyways
                if (transformationRequired) {
                    // setup the progress listener bridging GeoTools and Importer worlds
                    ProgressListener transformListener = new DefaultProgressListener() {

                        public void progress(float percent) {
                            double progress = (percent / 100 * transformQuota);
                            summary.setLayerProgress(resource.getLocalPart(),
                                    "Writing down image in directory:" + outputDirectoryPath,
                                    progress);
                            if (LOGGER.isLoggable(Level.INFO)) {
                                LOGGER.log(Level.INFO, "Writing down image in directory:"
                                        + outputDirectoryPath + " progress:" + progress + "  %");
                            }
                        }

                        public boolean isCanceled() {
                            return CoverageImporter.this.cancelled;
                        }

                        public void exceptionOccurred(Throwable exception) {
                            CoverageImporter.this.cancelled = true;
                        }

                    };

                    // TODO: setup copy to a temporary file, and back, in case we are not supposed to copy it
                    StringBuilder message = new StringBuilder("Rewriting input file ");
                    if (tiling.isEnabled()) {
                        message.append(" with tiling");
                    }
                    if (compression.isEnabled()) {
                        if (tiling.isEnabled()) {
                            message.append(" and");
                        }
                        message.append(" with compression");
                    }
                    layerProgress(resource, message.toString(), 20.0);
                    transformer.rebuild(inputFile, outputFile, compression, tiling,
                            transformListener);
                    layerProgress(resource, "Written down image in target dir", 50.0);
                }

                // Adds overviews if necessary
                if (overviewsEmbeddingRequired) {
                    layerProgress(resource, "Preparing to create overviews", 50.0);

                    ProgressListener overviewListener = new DefaultProgressListener() {

                        public void progress(float percent) {
                            double progress = transformQuota + (overviewQuota * percent / 100.0);
                            summary.setLayerProgress(resource.getLocalPart(), getTask().toString(),
                                    progress);
                        }

                        public boolean isCanceled() {
                            return CoverageImporter.this.cancelled;
                        }

                        public void exceptionOccurred(Throwable exception) {
                            CoverageImporter.this.cancelled = true;
                        }

                    };

                    // check which file we have to add overviews to
                    transformer.addOverviews(outputFile, compression, tiling, overviews,
                            overviewListener);
                }

                // did we work on a temporary file?
                if (temporaryFile) {
                    // delete the input file and replace it with the output one
                    inputFile.delete();
                    outputFile.renameTo(inputFile);
                    outputFile = null;
                }
            } finally {
                // if we had an error mid way we need to remove the temporary file
                if (temporaryFile && outputFile != null && !outputFile.delete()) {
                    LOGGER.log(Level.WARNING,
                            "Failed to delete the temporary file " + outputFile.getAbsolutePath());
                }
            }
        }

        // return the correct output file
        if (config.isCopy()) {
            return outputFile;
        } else {
            return inputFile;
        }
    }

    /**
     * Checks if the file can be left as is, or needs to transformed
     * 
     * @param transformer
     * @param inputFile
     * @return
     * @throws IOException
     */
    private boolean transformationRequired(CoverageTransformer transformer, File inputFile)
            throws IOException {
        CompressionConfiguration compression = this.config.getCompression();
        TilingConfiguration tiling = this.config.getTiling();

        // if compression enabled we don't have any way to preserve the file contents
        if (compression.isEnabled()) {
            return true;
        }

        // if the file is not a GeoTiff we are forced to transform it too
        if (!transformer.isGeotiff(inputFile)) {
            return true;
        }

        // tiling is more complicated, do we have to preserve existing inner tiles?
        if (tiling.isEnabled()) {
            if (!tiling.isRetainNativeTiles() || !transformer.isInnerTiled(inputFile)) {
                return true;
            }
        }

        // no need to perform any transformation on this file it seems
        return false;
    }

    private boolean overviewEmbeddingRequired(CoverageTransformer transformer, File inputFile, boolean transformationRequired)
            throws IOException {
        OverviewConfiguration overviews = this.config.getOverview();
        // do we have to preserve existing ones?
        if (overviews.isEnabled()) {
            // either we have overwrite the overviews, or the file does not have any, or we have to rewrite
            // the file anyways to force certain inner tiling or compression
            if (!overviews.isRetainOverviews() || !transformer.hasOverviews(inputFile) || transformationRequired) {
                return true;
            }
        }

        // no need to embed overviews
        return false;
    }

    private void copyFiles(Name resource, File currentFile, File outputFile) throws IOException {
        final File parentDirectory = new File(currentFile.getParent());
        final String fileName = FilenameUtils.getBaseName(currentFile.getCanonicalPath());
        final File[] filesToMove = parentDirectory.listFiles((FilenameFilter) FileFilterUtils
                .makeCVSAware(FileFilterUtils.makeSVNAware(FileFilterUtils
                        .makeFileOnly(new WildcardFileFilter(Arrays.asList(fileName + ".tfw",
                                fileName + ".prj", fileName + ".wld", fileName + ".tif.ovr",
                                fileName + ".tiff.ovr"), IOCase.INSENSITIVE)))));
        try {
            // move original file then siblings
            layerProgress(resource, "Copying file:" + currentFile.getCanonicalPath(), 10.0);
            FileUtils.copyFile(currentFile, outputFile);
            layerProgress(resource, "Copied original file:" + currentFile.getCanonicalPath(), 60.0);

            // move siblings
            if (filesToMove != null) {
                int i = 1, step = (int) (40.0 / filesToMove.length);
                for (File file : filesToMove) {
                    // move original file then siblings
                    FileUtils.copyFileToDirectory(file, config.getOutputDirectory());
                    layerProgress(resource,
                            "Copied original file: " + currentFile.getCanonicalPath(), 60.0 + step);
                    i++;
                }
            }
            layerProgress(resource, "Done with file:" + currentFile.getCanonicalPath(), 90.0);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            summary.setLayerProgress(
                    resource.getLocalPart(),
                    "Error moving file:" + currentFile.getCanonicalPath() + "\n"
                            + e.getLocalizedMessage(), 100.0);
        }
    }

    /**
     * Sets the progress on the layer, or bails out in case the import had been cancelled
     * 
     * @param resource
     * @param message
     * @param progress
     * @return
     */
    void layerProgress(Name resource, String message, double progress) throws CancelException {
        if (!cancelled) {
            summary.setLayerProgress(resource.getLocalPart(), message, progress);
        } else {
            throw new CancelException();
        }
    }

    /**
     * Exception used to bail out of any import activity and close up the import
     * 
     * @author aaime
     * 
     */
    static class CancelException extends Exception {

        /**
         * 
         */
        private static final long serialVersionUID = 5387428822124207090L;

    }

}
