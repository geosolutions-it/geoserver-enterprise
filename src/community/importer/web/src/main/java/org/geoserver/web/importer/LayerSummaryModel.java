/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.importer.LayerSummary;
import org.geoserver.web.GeoServerApplication;


/**
 * A model that serializes the layer summary fully, and re-attaches it to the catalog on
 * deserialization
 *
 * @author Andrea Aime, GeoSolutions
 *
 */
@SuppressWarnings("serial")
public class LayerSummaryModel implements IModel
{
    LayerSummary summary;

    public LayerSummaryModel(LayerSummary layerInfo)
    {
        this.summary = layerInfo;
    }

    public Object getObject()
    {
        if (summary.getLayer() != null)
        {
            ResourceInfo resource = summary.getLayer().getResource();
            if (resource != null)
            {
                if (resource.getCatalog() == null)
                {
                    (new CatalogBuilder(GeoServerApplication.get().getCatalog())).attach(summary.getLayer());
                }
            }
        }

        return summary;
    }

    public void setObject(Object object)
    {
        this.summary = (LayerSummary) object;
    }

    public void detach()
    {
        // nothing specific to do
    }

}
