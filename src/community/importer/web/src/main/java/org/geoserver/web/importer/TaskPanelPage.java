/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.geoserver.importer.ImportSummary;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;


/**
 * Page showing the list of active import tasks
 *
 * @author Luca Morandini lmorandini@ieee.org
 */
@SuppressWarnings("serial")
public class TaskPanelPage extends ImporterSecuredPage
{

    private GeoServerTablePanel<ImportSummary> importers;

    private Link<Object> refreshLink;

    private Link<Object> clearLink;

    @SuppressWarnings("unchecked")
    public TaskPanelPage()
    {

        // Add widget to clear completed tasks
        this.clearLink = new Link<Object>("clear")
            {
                public void onClick()
                {
                    getPageManager().clear();
                    setResponsePage(TaskPanelPage.class);
                }
            };
        add(this.clearLink);

        // Add widget to refresh task list
        this.refreshLink = new Link<Object>("refresh")
            {
                public void onClick()
                {
                    setResponsePage(TaskPanelPage.class);
                }
            };
        add(this.refreshLink);

        // List of past and present importers
        TaskPanelProvider provider = new TaskPanelProvider(this);

        this.importers = new GeoServerTablePanel<ImportSummary>("importers", provider, false)
            {

                @Override
                protected Component getComponentForProperty(final String id, IModel itemModel, Property<ImportSummary> property)
                {

                    final ImportSummary summary = (ImportSummary) itemModel.getObject();

                    if (property == TaskPanelProvider.TASK)
                    {
                        return new Label(id, summary.getProject());
                    }
                    if (property == TaskPanelProvider.STATUS)
                    {
                        return new Label(id, new ParamResourceModel("TaskPanelPage.status" + summary.getStatus().toString(), TaskPanelPage.this));
                    }
                    if (property == TaskPanelProvider.IMPORTEDLAYERS)
                    {
                        return new Label(id, Integer.toString(summary.getSuccesses()));
                    }
                    if (property == TaskPanelProvider.TOTALLAYERS)
                    {
                        return new Label(id, Integer.toString(summary.getTotalLayers()));
                    }
                    if (property == TaskPanelProvider.DETAILSPAGE)
                    {
                        return new SimpleAjaxLink(id, new ParamResourceModel("detailsPage", TaskPanelPage.this))
                            {

                                @Override
                                public void onClick(AjaxRequestTarget target)
                                {
                                    setResponsePage(getPageManager().getPage(summary.getId()));
                                }

                            };
                    }

                    return null;
                }
            };

        importers.setSortable(false);
        importers.setFilterable(false);
        add(importers);

    }

    public ImporterPageManager getPageManager()
    {
        return (ImporterPageManager) this.getGeoServerApplication().getBean("importerPages");
    }
}
