/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.gwc.GWC;
import org.geowebcache.layer.TileLayer;

/**
 * A loadable model for {@link TileLayer}s used by {@link CachedLayerProvider}.
 * <p>
 * Warning, don't use it in a tabbed form or in any other places where you might need to keep the
 * modifications in a resource stable across page loads.
 * </p>
 */
@SuppressWarnings("serial")
class TileLayerDetachableModel extends LoadableDetachableModel<TileLayer> {

    private String name;

    public TileLayerDetachableModel(String layerName) {
        this.name = layerName;
    }

    @Override
    protected TileLayer load() {
        GWC facade = GWC.get();
        return facade.getTileLayerByName(name);
    }
}
