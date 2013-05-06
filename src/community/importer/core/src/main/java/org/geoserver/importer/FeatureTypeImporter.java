/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.shapefile.ShapefileDirectoryFactory;
import org.opengis.feature.type.Name;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.geoserver.importer.LayerImportStatus.DEFAULTED_SRS;
import static org.geoserver.importer.LayerImportStatus.DUPLICATE;
import static org.geoserver.importer.LayerImportStatus.MISSING_BBOX;
import static org.geoserver.importer.LayerImportStatus.MISSING_NATIVE_CRS;
import static org.geoserver.importer.LayerImportStatus.NO_SRS_MATCH;
import static org.geoserver.importer.LayerImportStatus.SUCCESS;


/**
 * <p>
 * Tries to import all or some of the feature types in a datastore, provides the ability to observe
 * the process and to stop it prematurely.
 * </p>
 * <p>
 * It is advised to run it into its own thread
 * </p>
 *
 * $Id: FeatureTypeImporter.java 174 2012-01-23 15:11:17Z alessio $
 *
 * @author Luca Morandini lmorandini@ieee.org
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */

public class FeatureTypeImporter extends Importer
{

    DataStoreInfo storeInfo;

    private boolean copy = false;

    private String indirectory = "";

    private String outdirectory = "";

    /**
     * Cosntructor
     *
     * @param id
     *            Id of importer
     * @param tm
     *            Object that manages import thrads
     * @param store
     *            The feature data store
     * @param defaultSRS
     *            The default SRS to use when data have none
     * @param resources
     *            The list of resources to import. Use {@code null} to import all available ones
     * @param catalog
     *            The GeoServer catalog
     * @param workspaceNew
     *            Marks the workspace as newly created and ready for rollback
     * @param storeNew
     *            Marks the store as newly created and ready for rollback
     * @throws MalformedURLException
     */
    public FeatureTypeImporter(String id, ImporterThreadManager tm, DataStoreInfo store,
        String defaultSRS, Set<Name> resources, Catalog catalog, boolean workspaceNew,
        boolean storeNew, String outdirectory, boolean copy) throws MalformedURLException
    {

        super(id, tm, defaultSRS, resources, catalog, workspaceNew, storeNew);
        this.storeInfo = store;
        this.summary = new ImportSummary(id, storeInfo.getName(), workspaceNew, storeNew);
        this.copy = copy;
        this.outdirectory = outdirectory;
        if (copy)
        {
            this.indirectory =
                (new URL((String) this.storeInfo.getConnectionParameters().get(
                            ShapefileDirectoryFactory.URLP.key))).getFile();
        }
        else
        {
            this.indirectory = null;
        }
    }

    public String getProject()
    {
        return storeInfo.getName();
    }

    public void run()
    {
        // the importer is running in a background thread, make it use the same authentication
        // as the submitter
        if(authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        DataAccess da = null;
        try
        {
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(storeInfo.getWorkspace().getName());
            CatalogBuilder builder = new CatalogBuilder(catalog);
            da = storeInfo.getDataStore(null);

            StyleGenerator styles = new StyleGenerator(catalog);

            // Cast necessary due to some classpath oddity/geoapi issue, the compiler
            // complained about getNames() returning a List<Object>...
            List<Name> names = new ArrayList<Name>(da.getNames());

            // filter to the selected resources if necessary
            if (resources != null)
            {
                names.retainAll(resources);
            }

            // Initializes summary of import
            summary.setTotalLayers(names.size());

            // If data are to be copied, copies them to the output data directory and redirects the
            // data store to it
            try
            {
                if (copy)
                {
                    File inDir = new File(this.indirectory);
                    if (!inDir.isDirectory())
                    {
                        inDir = inDir.getParentFile();
                    }

                    File outDir = new File(this.outdirectory);

                    for (Name name : names)
                    {
                        // Get layer name and adds it to the summary
                        String layerName = name.getLocalPart();
                        summary.newLayer(layerName);

                        // FIXME: message should be i18n
                        summary.setLayerProgress(layerName, "Started copying", 0.0);

                        copyShapefile(layerName, inDir, outDir);

                        // FIXME: message should be i18n
                        summary.setLayerProgress(layerName, "Finished copying", 100.0);
                    }

                    // Makes the data store point to the output directory
                    this.storeInfo.getConnectionParameters().put(
                        ShapefileDirectoryFactory.URLP.key, outDir.toURI().toURL().toString());
                    catalog.save(this.storeInfo);
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Import process failed", e);
                summary.end(e);
                this.clearTask();

                return;
            }

            // For every dataset to import
            for (Name name : names)
            {

                // Get layer name and adds it to the summary
                String layerName = name.getLocalPart();
                summary.newLayer(layerName);

                // FIXME: message should be i18n
                summary.setLayerProgress(layerName, "Started adding feature type", 0.0);

                LayerInfo layer = null;
                try
                {
                    builder.setStore(storeInfo);

                    FeatureTypeInfo featureType = builder.buildFeatureType(name);
                    boolean geometryless = featureType.getFeatureType().getGeometryDescriptor() == null;
                    if (geometryless)
                    {
                        // geometryless case, fill in some random values just because we need them
                        featureType.setSRS("EPSG:4326");
                        featureType.setLatLonBoundingBox(ImportUtilities.WORLD);
                    }
                    else
                    {
                        builder.lookupSRS(featureType, true);
                        try
                        {
                            builder.setupBounds(featureType);
                        }
                        catch (Exception e)
                        {
                            LOGGER.log(Level.FINE, "Could not compute the layer bbox", e);
                        }
                    }

                    // FIXME: message should be i18n
                    summary.setLayerProgress(layerName, "Started adding layer", 50.0);

                    layer = builder.buildLayer(featureType);
                    layer.setDefaultStyle(styles.getStyle(featureType));

                    LayerImportStatus status = SUCCESS;

                    if (cancelled)
                    {
                        summary.end(true);
                        this.clearTask();

                        return;
                    }

                    // if we have a default
                    if ((layer.getResource().getSRS() == null) &&
                            (layer.getResource().getNativeCRS() != null) && (defaultSRS != null))
                    {
                        layer.getResource().setSRS(defaultSRS);
                        layer.getResource().setProjectionPolicy(
                            ProjectionPolicy.REPROJECT_TO_DECLARED);
                        status = DEFAULTED_SRS;
                    }

                    // handler common error conditions
                    if (catalog.getFeatureTypeByName(namespace, layerName) != null)
                    {
                        status = DUPLICATE;
                    }
                    else if ((layer.getResource().getSRS() == null) && (defaultSRS == null) &&
                            !geometryless)
                    {
                        if (layer.getResource().getNativeCRS() == null)
                        {
                            status = MISSING_NATIVE_CRS;
                        }
                        else
                        {
                            status = NO_SRS_MATCH;
                        }
                    }
                    else if (layer.getResource().getLatLonBoundingBox() == null)
                    {
                        status = MISSING_BBOX;
                    }
                    else
                    {
                        // try to save the layer
                        catalog.add(featureType);
                        try
                        {
                            catalog.add(layer);
                            // get a proxy that we can modify
                            layer = catalog.getLayer(layer.getId());
                        }
                        catch (Exception e)
                        {
                            // will be caught by the external try/catch, here we just try to undo
                            // the feature type saving (transactions, where are you...)
                            catalog.remove(featureType);
                            throw e;
                        }
                    }
                    // FIXME: i18n message
                    summary.completeLayer(layerName, layer, "Done!", status);
                }
                catch (Exception e)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                    {
                        LOGGER.log(Level.WARNING, "Import process failed", e);
                    }
                    // FIXME: i18n message
                    summary.completeLayer(layerName, layer,
                        "Exiting with an error" + e.getLocalizedMessage(), e);
                }

                // FIXME: message should be i18n
                summary.setLayerProgress(layerName, "Finished processing file: " + layerName, 100.0);

                if (cancelled)
                {
                    summary.end(true);
                    this.clearTask();

                    return;
                }
            }

            summary.end(false);
            this.clearTask();
        }
        catch (Exception e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
            {
                LOGGER.log(Level.WARNING, "Import process failed", e);
            }
            summary.end(e);
            this.clearTask();
        }
    }

    /**
     * Copies the files of a shapefile in a directory
     *
     * @param shp
     *            Name of the shapefile (without extension)
     * @param inDir
     *            Input directory
     * @param outDir
     *            Output directory
     * @param layerName
     * @throws IOException
     */
    @SuppressWarnings("serial")
    private void copyShapefile(final String shp, final File inDir, final File outDir) throws IOException
    {

        //
        // get list of files to copy
        //
        final File[] filesToMove = inDir.listFiles((FilenameFilter) FileFilterUtils.makeCVSAware(
                    FileFilterUtils.makeSVNAware(
                        FileFilterUtils.makeFileOnly(new WildcardFileFilter(
                                new ArrayList<String>()
                                {

                                    {
                                        add(shp + ".shp");
                                        add(shp + ".prj");
                                        add(shp + ".shx");
                                        add(shp + ".dbf");
                                        add(shp + ".sbn");
                                        add(shp + ".qix");
                                    }
                                }, IOCase.INSENSITIVE)))));
        // how many?
        if ((filesToMove != null) && (filesToMove.length > 0))
        {
            final int numFile = filesToMove.length;
            float step = 1.0f / numFile;
            float progress = 0.f;

            // copy
            for (File file : filesToMove)
            {
                final String fileName = file.getCanonicalPath();
                summary.setLayerProgress(shp, "Started copying file:" + fileName, progress * 100);
                FileUtils.copyFileToDirectory(file, outDir, true);
                progress += step;
                summary.setLayerProgress(shp, "Finished copying file:" + fileName, progress * 100);

            }
        }

    }
}
