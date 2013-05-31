/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.layer.TileLayer;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Provides a filtered, sorted view over GWC {@link TileLayer}s for {@link CachedLayersPage} using
 * {@link TileLayer} as data view.
 * 
 * @author groldan
 */
class CachedLayerProvider extends GeoServerDataProvider<TileLayer> {

    private static final long serialVersionUID = -8599398086587516574L;

    static final Property<TileLayer> TYPE = new AbstractProperty<TileLayer>("type") {

        private static final long serialVersionUID = 3215255763580377079L;

        @Override
        public ResourceReference getPropertyValue(TileLayer item) {
            return GWCIconFactory.getSpecificLayerIcon(item);
        }

        @Override
        public Comparator<TileLayer> getComparator() {
            return new Comparator<TileLayer>() {
                @Override
                public int compare(TileLayer o1, TileLayer o2) {
                    ResourceReference r1 = getPropertyValue(o1);
                    ResourceReference r2 = getPropertyValue(o2);
                    return r1.getName().compareTo(r2.getName());
                }
            };
        }

    };

    static final Property<TileLayer> NAME = new BeanProperty<TileLayer>("name", "name");

    static final Property<TileLayer> QUOTA_LIMIT = new AbstractProperty<TileLayer>("quotaLimit") {
        private static final long serialVersionUID = 5091453765439157623L;

        @Override
        public Object getPropertyValue(TileLayer item) {
            GWC gwc = GWC.get();

            return gwc.getQuotaLimit(item.getName());
        }
    };

    static final Property<TileLayer> QUOTA_USAGE = new AbstractProperty<TileLayer>("quotaUsed") {
        private static final long serialVersionUID = 3503671083744555325L;

        /**
         * 
         * @retun the used quota for the tile layer, may be {@code null}
         */
        @Override
        public Object getPropertyValue(TileLayer item) {
            GWC gwc = GWC.get();
            return gwc.getUsedQuota(item.getName());
        }
    };

    static final Property<TileLayer> ENABLED = new BeanProperty<TileLayer>("enabled", "enabled");

    static final Property<TileLayer> PREVIEW_LINKS = new AbstractProperty<TileLayer>("preview") {
        private static final long serialVersionUID = 4375670219356088450L;

        @Override
        public Object getPropertyValue(TileLayer item) {
            return item.getName();
        }

        @Override
        public boolean isSearchable() {
            return false;
        }

        @Override
        public Comparator<TileLayer> getComparator() {
            return null;
        }
    };

    static final Property<TileLayer> ACTIONS = new AbstractProperty<TileLayer>("actions") {
        private static final long serialVersionUID = 247933970378482802L;

        @Override
        public Object getPropertyValue(TileLayer item) {
            return item.getName();
        }

        @Override
        public boolean isSearchable() {
            return false;
        }

        @Override
        public Comparator<TileLayer> getComparator() {
            return null;
        }
    };

    @SuppressWarnings("unchecked")
    static final List<Property<TileLayer>> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            TYPE, NAME, QUOTA_LIMIT, QUOTA_USAGE, ENABLED, PREVIEW_LINKS, ACTIONS));

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getItems()
     */
    @Override
    protected List<TileLayer> getItems() {
        final GWC gwc = GWC.get();
        List<String> tileLayerNames = new ArrayList<String>(gwc.getTileLayerNames());
        return Lists.transform(tileLayerNames, new Function<String, TileLayer>() {

            @Override
            public TileLayer apply(String input) {
                return GWC.get().getTileLayerByName(input);
            }
        });
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getProperties()
     */
    @Override
    protected List<Property<TileLayer>> getProperties() {
        return PROPERTIES;
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object)
     */
    public IModel<TileLayer> newModel(final Object tileLayer) {
        return new TileLayerDetachableModel(((TileLayer) tileLayer).getName());
    }

    /**
     * @see org.geoserver.web.wicket.GeoServerDataProvider#getComparator
     */
    @Override
    protected Comparator<TileLayer> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }
}
