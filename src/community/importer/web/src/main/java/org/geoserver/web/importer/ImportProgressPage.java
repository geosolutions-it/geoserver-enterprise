/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.geoserver.importer.ImportSummary;
import org.geoserver.importer.ImportSummary.LayerProgress;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterThreadManager;


/**
 * Reports the import progress, the current layer, and allows to end the import mid way
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
@SuppressWarnings("serial")
public class ImportProgressPage extends ImporterSecuredPage
{

    protected String importerId;

    protected Label overallBar;

    protected Label overallPercentage;

    protected Label layerBar;

    protected Label layerPercentage;

    protected Label processingMessage;

    protected Label currentFile;

    protected WebMarkupContainer info;

    protected Model overallWidthModel;

    protected Model layerWidthModel;

    public ImportProgressPage(String importerKey)
    {
        this.importerId = importerKey;

        Importer importer = getImporter();

        // construction
        add(new Label("project", importer.getProject()));
        add(info = new WebMarkupContainer("info"));
        info.setOutputMarkupId(true);
        info.add(overallBar = new Label("overallBar", "0"));
        info.add(layerBar = new Label("layerBar", "0"));
        overallWidthModel = new Model("width: 5%;");
        overallBar.add(new AttributeModifier("style", overallWidthModel));
        layerWidthModel = new Model("width: 5%;");
        layerBar.add(new AttributeModifier("style", layerWidthModel));
        info.add(overallPercentage = new Label("overallPercentage", "5"));
        info.add(layerPercentage = new Label("layerPercentage", "0"));
        info.add(processingMessage = new Label("processingMessage", ""));
        info.add(currentFile = new Label("currentFile", ""));

        info.add(new AjaxLink("cancel")
            {
                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    Importer importer = getImporter();
                    importer.cancel();

                    setResponsePage(new ImportSummaryPage(importer.getSummary(), importerId));
                }
            });

        // comment this out if you need to hack on the HTML of a live page
        info.add(new AbstractAjaxTimerBehavior(Duration.milliseconds(500))
            {

                @Override
                protected void onTimer(AjaxRequestTarget target)
                {
                    ImportSummary summary = ImportProgressPage.this.getSummary();

                    if (summary != null)
                    {
                        if (summary.isCompleted())
                        {
                            setResponsePage(new ImportSummaryPage(summary, importerId));
                        }

                        // Sets overall progress
                        // we have the percentage go between 5 and 95. This way the user sees something
                        // is in progress
                        // at the beginning and does not have to wonder why we keep at 100 for a few
                        // seconds (happens
                        // if the last layer requires a costly SRS code lookup)
                        long perc = 5 + Math.round(100.0 * (summary.getProcessedLayers() + 1) /
                                summary.getTotalLayers());
                        if (perc > 90)
                        {
                            perc = 90;
                        }
                        overallWidthModel.setObject("width: " + perc + "%;");
                        overallPercentage.setDefaultModelObject(perc);
                        currentFile.setDefaultModelObject(summary.getCurrentLayer());

                        // Sets current layer progress (if possible)
                        LayerProgress currentLayerProgress = summary.getLayerProgress(summary.getCurrentLayer());
                        if (currentLayerProgress != null)
                        {
                            perc = Math.round(currentLayerProgress.getProgress());
                            layerWidthModel.setObject("width: " + perc + "%;");
                            layerPercentage.setDefaultModelObject(perc);
                            processingMessage.setDefaultModelObject(currentLayerProgress.getMessage());
                        }
                        else
                        {
                            LOGGER.fine("The progress of layer " + summary.getCurrentLayer() +
                                " cannot be retrieved ");
                        }

                        target.addComponent(info);
                    }
                }

            });
    }

    public Importer getImporter()
    {
        ImporterThreadManager manager = (ImporterThreadManager) getGeoServerApplication().getBean(
                "importerPool");
        Importer importer = manager.getImporter(this.importerId);

        return importer;
    }

    public ImportSummary getSummary()
    {
        ImporterPageManager pageManager = (ImporterPageManager) getGeoServerApplication().getBean("importerPages");

        return pageManager.getSummary(this.importerId);
    }

    /**
     * We want the indicator always on
     */
    @Override
    public String getAjaxIndicatorMarkupId()
    {
        return null;
    }

}
