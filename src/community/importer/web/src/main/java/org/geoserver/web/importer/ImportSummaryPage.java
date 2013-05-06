/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: ImportSummaryPage.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.web.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.RequestUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.ImportSummary;
import org.geoserver.importer.LayerImportStatus;
import org.geoserver.importer.LayerSummary;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.demo.PreviewLayer;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.ConfirmationAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSListPanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.geoserver.web.importer.ImportSummaryProvider.COMMANDS;
import static org.geoserver.web.importer.ImportSummaryProvider.ISSUES;
import static org.geoserver.web.importer.ImportSummaryProvider.LAYER;
import static org.geoserver.web.importer.ImportSummaryProvider.SRS;
import static org.geoserver.web.importer.ImportSummaryProvider.TYPE;


/**
 * Reports the import results in a table and allows the user to edit the and to preview the layers
 *
 * @author Andrea Aime, GeoSolutions
 *
 */
@SuppressWarnings("serial")
public class ImportSummaryPage extends ImporterSecuredPage
{

    private ModalWindow popupWindow;

    private GeoServerTablePanel<LayerSummary> summaryTable;

    private SimpleAjaxLink declareSRSLink;

    public ImportSummaryPage(final ImportSummary summary, String importerId)
    {

        // the synthetic results
        IModel summaryMessage;
        Exception error = summary.getError();
        if (error != null)
        {
            String errorSummary = error.getClass().getSimpleName() + ", " + error.getMessage();
            summaryMessage = new ParamResourceModel("summaryError", this, errorSummary);
            // Hide undo link
            add(undoLink(summary).setVisible(false));
        }
        else if (summary.getProcessedLayers() == 0)
        {
            summaryMessage = new ParamResourceModel("summaryCancelled", this);
            // no undo link in this case
            add(new Label("undo", ""));
        }
        else
        {
            if (summary.getFailures() > 0)
            {
                if (summary.isCompleted())
                {
                    summaryMessage = new ParamResourceModel("summaryFailures", this, summary.getSuccesses(), summary.getTotalLayers(), summary.getProject(),
                            summary.getFailures());
                }
                else
                {
                    summaryMessage = new ParamResourceModel("summaryPartialFailures", this, summary.getSuccesses(), summary.getTotalLayers(), summary.getProject(),
                            summary.getFailures());
                }
            }
            else
            {
                if (summary.isCompleted())
                {
                    summaryMessage = new ParamResourceModel("summarySuccess", this, summary.getSuccesses(), summary.getTotalLayers(), summary.getProject());
                }
                else
                {
                    summaryMessage = new ParamResourceModel("summaryPartialSuccess", this, summary.getSuccesses(), summary.getTotalLayers(), summary.getProject());
                }
            }

            // show undo link
            add(undoLink(summary));
        }
        add(new Label("summary", summaryMessage));

        // the popup window
        popupWindow = new ModalWindow("popup");
        add(popupWindow);

        // the declare SRS link
        declareSRSLink = popupLink("declareSRS", new ParamResourceModel("declareSRS", this),
                srsListSelectionPanel());
        declareSRSLink.getLink().setEnabled(false);
        declareSRSLink.setOutputMarkupId(true);
        add(declareSRSLink);

        // the list of imported layers
        ImportSummaryProvider provider = new ImportSummaryProvider(summary.getLayers());
        summaryTable = new GeoServerTablePanel<LayerSummary>("importSummary", provider, true)
            {

                @Override
                protected Component getComponentForProperty(String id, IModel itemModel, Property<LayerSummary> property)
                {
                    final LayerSummary layerSummary = (LayerSummary) itemModel.getObject();
                    final CatalogIconFactory icons = CatalogIconFactory.get();
                    LayerInfo layer = layerSummary.getLayer();
                    if (property == LAYER)
                    {
                        Fragment f = new Fragment(id, "edit", ImportSummaryPage.this);

                        // keep the last modified name if possible
                        IModel label;
                        if (layerSummary.getLayer() != null)
                        {
                            label = new Model(layerSummary.getLayer().getName());
                        }
                        else
                        {
                            label = new Model(layerSummary.getLayerName());
                        }

                        // build the edit link
                        Link editLink = editLink(layerSummary, label);
                        editLink.setEnabled(layer != null);
                        // also set a tooltip explaining what this action does
                        editLink.add(new AttributeModifier("title", true, new ParamResourceModel(
                                    "edit", this, label.getObject())));
                        f.add(editLink);

                        return f;
                    }
                    else if (property == TYPE)
                    {
                        if (layer != null)
                        {
                            // show icon type or an error icon if anything went wrong
                            ResourceReference icon;
                            IModel title = new Model(getTypeTooltip(layer));
                            if (layerSummary.getStatus().successful())
                            {
                                icon = icons.getSpecificLayerIcon(layer);
                                title = new Model(getTypeTooltip(layer));
                            }
                            else
                            {
                                icon = icons.getDisabledIcon();
                                title = ISSUES.getModel(itemModel);
                            }

                            Fragment f = new Fragment(id, "iconFragment", ImportSummaryPage.this);
                            Image image = new Image("icon", icon);
                            image.add(new AttributeModifier("title", true, title));
                            image.add(new AttributeModifier("alt", true, title));
                            f.add(image);

                            return f;
                        }
                        else
                        {
                            // no icon, no description
                            return new Label(id, "");
                        }
                    }
                    else if (property == ISSUES)
                    {
                        if (layerSummary.getError() != null)
                        {
                            return new Label(id, layerSummary.getError().getMessage());
                        }
                        if (layerSummary.getStatus() != LayerImportStatus.NO_SRS_MATCH)
                        {
                            return new Label(id, property.getModel(itemModel));
                        }

                        Fragment f = new Fragment(id, "noSRSMatch", ImportSummaryPage.this);
                        f.add(new Label("issue", property.getModel(itemModel)));
                        f.add(getLayerWKTLink(itemModel));

                        return f;
                    }
                    else if (property == SRS)
                    {
                        if (layerSummary.getStatus().successful())
                        {
                            return new Label(id, property.getModel(itemModel));
                        }
                        else if ((layerSummary.getStatus() == LayerImportStatus.MISSING_NATIVE_CRS) ||
                                (layerSummary.getStatus() == LayerImportStatus.NO_SRS_MATCH))
                        {
                            return popupLink(id, new ParamResourceModel("declareSRS", this),
                                    srsListLayerPanel(itemModel));
                        }
                        else
                        {
                            Fragment f = new Fragment(id, "edit", ImportSummaryPage.this);

                            // build the edit link
                            Link editLink = editLink(layerSummary, new ParamResourceModel("directFix",
                                        this));
                            editLink.setEnabled(layer != null);

                            f.add(editLink);
                        }
                    }
                    else if (property == COMMANDS)
                    {
                        boolean geometryless = false;
                        boolean vector = false;
                        if (layerSummary.getLayer() != null)
                        {
                            ResourceInfo resource = layerSummary.getLayer().getResource();
                            if (resource instanceof FeatureTypeInfo)
                            {
                            	vector = true;
                                try
                                {
                                    FeatureType featureType = ((FeatureTypeInfo) resource).getFeatureType();
                                    geometryless = featureType.getGeometryDescriptor() == null;
                                }
                                catch (Exception e)
                                {
                                    geometryless = true;
                                }
                            } 
                        }

                        if (layerSummary.getStatus().successful() && !geometryless)
                        {
                            Fragment f = new Fragment(id, "preview", ImportSummaryPage.this);
                            PreviewLayer preview = new PreviewLayer(layer);
                            f.add(new ExternalLink("ol", preview.getWmsLink() +
                                    "&format=application/openlayers"));
                            f.add(new ExternalLink("ge", "../wms/kml?layers=" + layer.getName()));

                            // Link to the styler only for vector layers
                            ExternalLink stylerLink = new ExternalLink("styler", "../www/styler/index.html");
                            stylerLink.setVisible(vector);
                            f.add(stylerLink);

                            return f;
                        }
                        else
                        {
                            return new Label(id, "");
                        }
                    }

                    return null;
                }

                @Override
                protected void onSelectionUpdate(AjaxRequestTarget target)
                {
                    declareSRSLink.getLink().setEnabled(getSelection().size() > 0);
                    target.addComponent(declareSRSLink);
                }

            };
        summaryTable.setOutputMarkupId(true);
        summaryTable.setFilterable(false);
        add(summaryTable);
    }

    /**
     * Rolls back the import and redirects to the initial page
     *
     * @param summary
     * @return
     */
    private ConfirmationAjaxLink undoLink(final ImportSummary summary)
    {
        return new ConfirmationAjaxLink("undo", null, new ParamResourceModel("rollback", this),
                new ParamResourceModel("confirmRollback", this))
            {

                @Override
                protected void onClick(AjaxRequestTarget target)
                {
                    Catalog catalog = getCatalog();
                    ImporterCascadeDeleteVisitor deleteVisitor = new ImporterCascadeDeleteVisitor(
                            catalog);
                    String project = summary.getProject();
                    if (summary.isWorkspaceNew())
                    {
                        WorkspaceInfo ws = catalog.getWorkspaceByName(project);
                        if (ws != null)
                        {
                            ws.accept(deleteVisitor);
                        }
                    }
                    else if (summary.isStoreNew())
                    {
                        StoreInfo si = catalog.getStoreByName((WorkspaceInfo) null, project,
                                StoreInfo.class);
                        if (si != null)
                        {
                            si.accept(deleteVisitor);
                        }
                    }
                    else
                    {
                        // Just removes the layers we created
                        for (LayerSummary layer : summary.getLayers())
                        {
                            LayerInfo layerInfo = layer.getLayer();
                            if (layerInfo != null)
                            {
                                ResourceInfo resourceInfo = layerInfo.getResource();
                                if (resourceInfo != null)
                                {
                                    CoverageStoreInfo storeInfo = (CoverageStoreInfo) resourceInfo.getStore();
                                    deleteVisitor.visit(storeInfo);
                                }
                            }
                        }
                    }

                    // remove summary from task
                    ImporterPageManager pageManager = (ImporterPageManager) getGeoServerApplication().getBean("importerPages");
                    pageManager.removeSummary(summary.getId());
                    // move to next
                    setResponsePage(StoreChooserPage.class, new PageParameters("afterCleanup=true"));
                }
            };
    }

    Link editLink(final LayerSummary layerSummary, IModel labelModel)
    {
        Link link = new Link("edit")
            {

                @Override
                public void onClick()
                {
                    Page p = new ResourceConfigurationPage(layerSummary.getLayer(), true)
                        {
                            @Override
                            protected void onSuccessfulSave()
                            {
                                setResponsePage(ImportSummaryPage.this);
                                layerSummary.setStatus(LayerImportStatus.SUCCESS);
                                layerSummary.updateLayer(getCatalog());
                            }

                            @Override
                            protected void onCancel()
                            {
                                setResponsePage(ImportSummaryPage.this);
                            }
                        };
                    setResponsePage(p);
                }

            };
        link.add(new Label("name", labelModel));

        return link;
    }

    String getTypeTooltip(LayerInfo layer)
    {
        try
        {
            String type = null;

            if (layer.getResource() instanceof FeatureTypeInfo)
            {
                FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();
                GeometryDescriptor gd = fti.getFeatureType().getGeometryDescriptor();
                if (gd != null)
                {
                    type = gd.getType().getBinding().getSimpleName();
                }
                if (type != null)
                {
                    return new ParamResourceModel("geomtype." + type, ImportSummaryPage.this).getString();
                }
                else
                {
                    return "geomtype.null";
                }
            }
            else
            {
                return "geomtype.raster";
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Could not compute the geom type tooltip", e);

            return "geomtype.error";
        }
    }

    Component getLayerWKTLink(IModel layerSummaryModel)
    {
        return new SimpleAjaxLink("seeWKT", layerSummaryModel, new ParamResourceModel("seeWKT",
                    this))
            {

                @Override
                protected void onClick(AjaxRequestTarget target)
                {
                    popupWindow.setInitialHeight(375);
                    popupWindow.setInitialWidth(525);

                    LayerSummary summary = (LayerSummary) getDefaultModel().getObject();
                    CoordinateReferenceSystem crs = summary.getLayer().getResource().getNativeCRS();
                    popupWindow.setContent(new CRSPanel.WKTPanel(popupWindow.getContentId(), crs));
                    if (crs != null)
                    {
                        popupWindow.setTitle(crs.getName().toString());
                    }
                    popupWindow.show(target);
                }
            };
    }

    SimpleAjaxLink popupLink(String id, final IModel label, final Component windowContent)
    {
        return new SimpleAjaxLink(id, label)
            {

                @Override
                protected void onClick(AjaxRequestTarget target)
                {
                    popupWindow.setContent(windowContent);
                    popupWindow.setTitle(new ParamResourceModel("selectSRS", ImportSummaryPage.this));
                    popupWindow.show(target);
                }
            };
    }

    /**
     * Builds the srs list panel component for a single layer
     */
    SRSListPanel srsListLayerPanel(final IModel layerSummaryModel)
    {
        SRSListPanel srsList = new SRSListPanel(popupWindow.getContentId())
            {

                @Override
                protected void onCodeClicked(AjaxRequestTarget target, String epsgCode)
                {
                    popupWindow.close(target);

                    LayerSummary summary = (LayerSummary) layerSummaryModel.getObject();
                    forceEpsgCode(epsgCode, summary);

                    target.addComponent(summaryTable);
                }
            };
        srsList.setCompactMode(true);

        return srsList;
    }

    /**
     * Builds the srs list panel component for a single layer
     */
    SRSListPanel srsListSelectionPanel()
    {
        SRSListPanel srsList = new SRSListPanel(popupWindow.getContentId())
            {

                @Override
                protected void onCodeClicked(AjaxRequestTarget target, String epsgCode)
                {
                    popupWindow.close(target);

                    for (LayerSummary summary : summaryTable.getSelection())
                    {
                        forceEpsgCode(epsgCode, summary);
                    }

                    summaryTable.clearSelection();
                    target.addComponent(summaryTable);
                }
            };
        srsList.setCompactMode(true);

        return srsList;
    }

    /**
     * Sets the EPSG code on a layer and saves it
     *
     * @param epsgCode
     * @param catalog
     * @param summary
     */
    void forceEpsgCode(String epsgCode, LayerSummary summary)
    {
        Catalog catalog = getCatalog();
        LayerInfo layer = summary.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:" + epsgCode);
        try
        {
            new CatalogBuilder(getCatalog()).setupBounds(resource);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.FINE, "Issue occurred while computing the bounding boxes", e);
        }
        if (resource.getLatLonBoundingBox() == null)
        {
            summary.setStatus(LayerImportStatus.MISSING_BBOX);
        }
        else
        {
            summary.setStatus(LayerImportStatus.SUCCESS);
        }
        if ((layer.getId() == null) || (catalog.getLayer(layer.getId()) == null))
        {
            catalog.add(resource);
            catalog.add(layer);
        }
        else
        {
            catalog.save(resource);
            catalog.save(layer);
        }
        summary.updateLayer(catalog);
    }

}
