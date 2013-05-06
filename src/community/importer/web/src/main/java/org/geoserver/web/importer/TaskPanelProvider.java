/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: TaskPanelProvider.java 174 2012-01-23 15:11:17Z alessio $
 */

package org.geoserver.web.importer;

import java.util.Arrays;
import java.util.List;

import org.geoserver.importer.ImportSummary;
import org.geoserver.web.wicket.GeoServerDataProvider;


/**
 * Provides a list of resources for a specific data store
 *
 * @author Luca Morandini lmorandini@ieee.org
 *
 */
@SuppressWarnings("serial")
public class TaskPanelProvider extends GeoServerDataProvider<ImportSummary>
{

    public static final Property<ImportSummary> TASK = new BeanProperty<ImportSummary>("task", "task");

    public static final Property<ImportSummary> STATUS = new BeanProperty<ImportSummary>("status", "status");

    public static final Property<ImportSummary> TOTALLAYERS = new BeanProperty<ImportSummary>("totalLayers",
            "totalLayers");

    public static final Property<ImportSummary> IMPORTEDLAYERS = new BeanProperty<ImportSummary>("importedLayers",
            "importedLayers");

    public static final Property<ImportSummary> DETAILSPAGE = new BeanProperty<ImportSummary>("detailsPage",
            "detailsPage");

    static final List<Property<ImportSummary>> PROPERTIES = Arrays.asList(TASK, STATUS, TOTALLAYERS, IMPORTEDLAYERS,
            DETAILSPAGE);

    TaskPanelPage page;

    public TaskPanelProvider(TaskPanelPage page)
    {
        this.page = page;
    }

    @Override
    public int size()
    {
        return this.page.getPageManager().getSummaries().size();
    }

    @Override
    public List<ImportSummary> getItems()
    {
        return this.page.getPageManager().getSummaries();
    }

    @Override
    public List<ImportSummary> getFilteredItems()
    {
        return this.getItems();
    }

    @Override
    public List<Property<ImportSummary>> getProperties()
    {
        return PROPERTIES;
    }

}
