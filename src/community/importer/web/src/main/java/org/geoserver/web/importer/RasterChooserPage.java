/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: RasterChooserPage.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer;

import java.io.File;
import java.rmi.server.UID;
import java.util.Set;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.importer.CompressionConfiguration;
import org.geoserver.importer.CoverageImportConfiguration;
import org.geoserver.importer.CoverageImporter;
import org.geoserver.importer.ImporterThreadManager;
import org.geoserver.importer.OverviewConfiguration;
import org.geoserver.importer.TilingConfiguration;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.opengis.feature.type.Name;


/**
 * Allows to choose the raster layers that need to be imported
 *
 * @author Andrea Aime, GeoSolutions
 * @author Luca Morandini lmorandini@ieee.org
 */
@SuppressWarnings("serial")
public class RasterChooserPage extends ImporterSecuredPage
{

    private GeoServerTablePanel<Resource> layers;

    private String wsName;

    private boolean workspaceNew;

    private boolean storeNew;

    private boolean copy = false;

    private boolean tile = false;

    private boolean overview = false;

    private String directory;

    private String outdirectory;

    private String compressiontype;

    private int compressionratio;

    private int tilewidth;

    private int tileheight;

    private boolean rettile = false;

    private int noverview;

    private int downsamplestep;

    private boolean retoverview = false;

    private boolean extoverview = false;

    private String subsamplealgorithm;

    private String defaultsrs;

    /*
     * Constructor
     */
    public RasterChooserPage(PageParameters params)
    {
        this.wsName = params.getString("workspace");
        this.directory = params.getString("directory");
        this.outdirectory = params.getString("outdirectory");
        this.workspaceNew = params.getBoolean("workspaceNew");
        this.storeNew = params.getBoolean("storeNew");
        this.copy = params.getBoolean("copy");
        this.tile = params.getBoolean("tile");
        this.overview = params.getBoolean("overview");
        this.compressiontype = params.getString("compressiontype");
        this.compressionratio = params.getAsInteger("compressionratio");
        this.tilewidth = params.getAsInteger("tilewidth");
        this.tileheight = params.getAsInteger("tileheight");
        this.rettile = params.getBoolean("rettile");
        this.noverview = params.getAsInteger("noverview");
        this.downsamplestep = params.getAsInteger("downsamplestep");
        this.subsamplealgorithm = params.getString("subsamplealgorithm");
        this.retoverview = params.getBoolean("retoverview");
        this.extoverview = params.getBoolean("extoverview");
        this.defaultsrs = params.getString("defaultsrs");

        RasterChooserProvider provider = new RasterChooserProvider(this.directory, this.wsName);
        if (provider.size() == 0)
        {
            error(new ParamResourceModel("storeEmpty", this, "", wsName).getString());
        }

        // Builds the GUI
        Form form = new Form("form", new CompoundPropertyModel(this));
        this.add(form);
        layers = new GeoServerTablePanel<Resource>("layerChooser", provider, true)
            {

                @Override
                protected Component getComponentForProperty(String id, IModel itemModel, Property<Resource> property)
                {
                    Resource resource = (Resource) itemModel.getObject();
                    if (property == RasterChooserProvider.TYPE)
                    {
                        return new Icon(id, resource.getIcon());
                    }
                    else if (property == RasterChooserProvider.NAME)
                    {
                        return new Label(id, property.getModel(itemModel));
                    }

                    return null;
                }
            };
        layers.setPageable(false);
        layers.setItemsPerPage(provider.fullSize() > 1 ? provider.fullSize() : 1);
        layers.setFilterable(false);
        layers.selectAll();
        form.add(layers);

        // submit
        SubmitLink submitLink = submitLink();
        form.add(submitLink);
        form.setDefaultButton(submitLink);
    }

    SubmitLink submitLink()
    {
        return new SubmitLink("import")
            {

                @Override
                public void onSubmit()
                {
                    // Executes the actual import of raster data
                    try
                    {
                        // Grabs the selection
                        Set<Name> names = new java.util.HashSet<Name>();
                        for (Resource r : layers.getSelection())
                        {
                            names.add(r.getName());
                        }

                        // if nothing was selected we need to go back
                        if (names.size() == 0)
                        {
                            error(new ParamResourceModel("selectionEmpty", RasterChooserPage.this).getString());

                            return;
                        }

                        // Builds and runs the importer
                        String id = new UID().toString();
                        
                        // configuer the importer and start it
                        CoverageImportConfiguration config = new CoverageImportConfiguration();
                        config.setImageFile(new File(directory));
                        config.setOutputDirectory(new File(outdirectory));
                        config.setWorkspace(wsName);
                        config.setCopy(copy);
                        TilingConfiguration tc = config.getTiling();
                        tc.setEnabled(tile);
                        tc.setRetainNativeTiles(rettile);
                        tc.setTileWidth(tilewidth);
                        tc.setTileHeight(tileheight);
                        CompressionConfiguration cc = config.getCompression();
                        cc.setRatio(compressionratio);
                        cc.setType(compressiontype);
                        OverviewConfiguration oc = config.getOverview();
                        oc.setEnabled(overview);
                        oc.setDownsampleStep(downsamplestep);
                        oc.setExternalOverviews(extoverview);
                        oc.setNumOverviews(noverview);
                        oc.setRetainOverviews(retoverview);
                        oc.setSubsampleAlgorithm(subsamplealgorithm);
                        
                        ImporterThreadManager threadManager = (ImporterThreadManager) getGeoServerApplication().getBean("importerPool");
						CoverageImporter importer = new CoverageImporter(id, threadManager, defaultsrs, names,
                            getCatalog(), workspaceNew, storeNew, config);
                        ImporterThreadManager manager = threadManager;
                        final String result = manager.startImporter(id, importer);
                        if (result == null)
                        {
                            // FIXME i18N
                            RasterChooserPage.this.error("Import task has been rejected!");

                        }
                        else
                        {
                            // Adds the progress page of the import
                            ImportProgressPage progressPage = new ImportProgressPage(id);
                            ImporterPageManager pageManager = (ImporterPageManager) getGeoServerApplication().getBean("importerPages");
                            pageManager.addProgressPage(id, progressPage);
                            pageManager.addSummary(id, importer.getSummary());
                            setResponsePage(progressPage);
                        }


                    }
                    catch (Exception e)
                    {
                        LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                    }

                }
            };
    }
}
