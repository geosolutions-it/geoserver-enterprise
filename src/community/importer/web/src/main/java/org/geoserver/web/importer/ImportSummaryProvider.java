/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.importer.LayerSummary;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;


/**
 * Data source for the {@link ImportSummaryPage}
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class ImportSummaryProvider extends GeoServerDataProvider<LayerSummary>
{
    static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ImportSummaryProvider.class);

    static final Property<LayerSummary> LAYER = new BeanProperty<LayerSummary>("name", "layerName");

    static final Property<LayerSummary> ISSUES = new AbstractProperty<LayerSummary>("details")
        {
            public Object getPropertyValue(LayerSummary item)
            {
                return new ResourceModel("ImportSummaryPage." + item.getStatus());
            }

            @Override
            public Comparator<LayerSummary> getComparator()
            {
                return new Comparator<LayerSummary>()
                    {

                        public int compare(LayerSummary o1, LayerSummary o2)
                        {
                            ResourceModel r1 = (ResourceModel) getPropertyValue(o1);
                            ResourceModel r2 = (ResourceModel) getPropertyValue(o2);
                            String s1 = (String) r1.getObject();
                            String s2 = (String) r2.getObject();

                            return s1.compareTo(s2);
                        }

                    };
            }
        };

    static final Property<LayerSummary> SRS = new BeanProperty<LayerSummary>("SRS",
            "layer.resource.SRS");

    static final Property<LayerSummary> TYPE = new AbstractProperty<LayerSummary>("type")
        {

            public Object getPropertyValue(LayerSummary item)
            {
                try
                {
                    if (item.getLayer() == null)
                    {
                        return null;
                    }

                    FeatureType type = ((FeatureTypeInfo) item.getLayer().getResource()).getFeatureType();
                    GeometryDescriptor gd = type.getGeometryDescriptor();
                    if (gd == null)
                    {
                        return null;
                    }
                    else
                    {
                        return gd.getType().getBinding().getSimpleName();
                    }
                }
                catch (IOException e)
                {
                    LOGGER.log(Level.WARNING, "Problems occurred computing the geometry type");

                    return null;
                }
            }
        };

    static final Property<LayerSummary> COMMANDS = new PropertyPlaceholder<LayerSummary>("commands");

    private List<LayerSummary> layers;

    public ImportSummaryProvider(List<LayerSummary> layers)
    {
        this.layers = new ArrayList<LayerSummary>(layers);
    }

    @Override
    protected List<LayerSummary> getItems()
    {
        Collections.sort(layers, new LayerSummaryComparator());

        return layers;
    }

    @Override
    protected List getProperties()
    {
        return Arrays.asList(TYPE, LAYER, ISSUES, SRS, COMMANDS);
    }

    public IModel newModel(Object object)
    {
        return new LayerSummaryModel((LayerSummary) object);
    }

    /**
     * Sorts layers so that the first ones are failures. The secondary sort criteria is the layer
     * name
     */
    static class LayerSummaryComparator implements Comparator<LayerSummary>
    {

        public int compare(LayerSummary s1, LayerSummary s2)
        {
            if (s1.getStatus().successful() == s2.getStatus().successful())
            {
                return s1.getLayerName().compareTo(s2.getLayerName());
            }
            else
            {
                if (s1.getStatus().successful())
                {
                    return 1;
                }
                else
                {
                    return -1;
                }
            }
        }

    }

}
