/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.web.data.resource.LayerEditTabPanelInfo;

public class LayerEditCacheOptionsTabPanelInfo extends LayerEditTabPanelInfo {

    private static final long serialVersionUID = 7917940832781227130L;

    @Override
    public GeoServerTileLayerInfoModel createOwnModel(
            final IModel<? extends ResourceInfo> resourceModel, final IModel<LayerInfo> layerModel,
            final boolean isNew) {

        LayerInfo layerInfo = layerModel.getObject();
        GeoServerTileLayerInfo tileLayerInfo;

        final GWC mediator = GWC.get();
        final GWCConfig defaultSettings = mediator.getConfig();

        final GeoServerTileLayer tileLayer = isNew ? null : mediator.getTileLayer(layerInfo);

        if (isNew || tileLayer == null) {
            /*
             * Ensure a sane config for defaults, in case automatic cache of new layers is defined
             * and the defaults is misconfigured
             */
            final GWCConfig saneDefaults = defaultSettings.saneConfig();
            tileLayerInfo = TileLayerInfoUtil.loadOrCreate(layerInfo, saneDefaults);
        } else {
            GeoServerTileLayerInfo info = ((GeoServerTileLayer) tileLayer).getInfo();
            tileLayerInfo = info.clone();
        }

        tileLayerInfo.setEnabled(true);
        final boolean initWithTileLayer = (isNew && defaultSettings.isCacheLayersByDefault())
                || tileLayer != null;

        if (!initWithTileLayer) {
            tileLayerInfo.setId(null);// indicate not to create the tile layer
        }

        return new GeoServerTileLayerInfoModel(tileLayerInfo, isNew);
    }
}
