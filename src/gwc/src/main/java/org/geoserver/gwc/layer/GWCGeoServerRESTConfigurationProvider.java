/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import javax.servlet.http.HttpServletResponse;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geowebcache.config.XMLConfigurationProvider;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.service.HttpErrorCodeException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * GWC xml configuration {@link XMLConfigurationProvider contributor} so that GWC knows how to
 * marshal and unmarshal {@link GeoServerTileLayer} instances for its REST API.
 * <p>
 * Note this provider is different than {@link GWCGeoServerConfigurationProvider}, which is used to
 * save the configuration objects. In contrast, this one is used only for the GWC REST API, as it
 * doesn't distinguish betwee {@link TileLayer} objects and tile layer configuration objects (as the
 * GWC/GeoServer integration does with {@link GeoServerTileLayer} and {@link GeoServerTileLayerInfo}
 * ).
 * 
 */
public class GWCGeoServerRESTConfigurationProvider implements XMLConfigurationProvider {

    private final Catalog catalog;

    public GWCGeoServerRESTConfigurationProvider(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerLayer", GeoServerTileLayer.class);
        xs.registerConverter(new RESTConverterHelper());
        return xs;
    }

    /**
     * @author groldan
     * 
     */
    private final class RESTConverterHelper implements Converter {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
            return GeoServerTileLayer.class.equals(type);
        }

        @Override
        public GeoServerTileLayer unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {

            Object current = new GeoServerTileLayerInfoImpl();
            Class<?> type = GeoServerTileLayerInfo.class;
            GeoServerTileLayerInfo info = (GeoServerTileLayerInfo) context.convertAnother(current,
                    type);
            String id = info.getId();
            String name = info.getName();
            if (id != null && id.length() == 0) {
                id = null;
            }
            if (name != null && name.length() == 0) {
                name = null;
            }
            if (name == null) {// name is mandatory
                throw new HttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST,
                        "Layer name not provided");
            }
            LayerInfo layer = null;
            LayerGroupInfo layerGroup = null;
            if (id != null) {
                layer = catalog.getLayer(id);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroup(id);
                    if (layerGroup == null) {
                        throw new HttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST,
                                "No GeoServer Layer or LayerGroup exists with id '" + id + "'");
                    }
                }
            } else {
                layer = catalog.getLayerByName(name);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroupByName(name);
                    if (layerGroup == null) {
                        throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_FOUND,
                                "GeoServer Layer or LayerGroup '" + name + "' not found");
                    }
                }
            }

            final String actualId = layer != null ? layer.getId() : layerGroup.getId();
            final String actualName = layer != null ? GWC.tileLayerName(layer) : GWC
                    .tileLayerName(layerGroup);

            if (id != null && !name.equals(actualName)) {
                throw new HttpErrorCodeException(HttpServletResponse.SC_BAD_REQUEST,
                        "Layer with id '" + id + "' found but name does not match: '" + name
                                + "'/'" + actualName + "'");
            }

            info.setId(actualId);
            info.setName(actualName);

            GeoServerTileLayer tileLayer;
            final GridSetBroker gridsets = GWC.get().getGridSetBroker();
            if (layer != null) {
                tileLayer = new GeoServerTileLayer(layer, gridsets, info);
            } else {
                tileLayer = new GeoServerTileLayer(layerGroup, gridsets, info);
            }
            return tileLayer;
        }

        @Override
        public void marshal(/* GeoServerTileLayer */Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            GeoServerTileLayer tileLayer = (GeoServerTileLayer) source;
            GeoServerTileLayerInfo info = tileLayer.getInfo();
            context.convertAnother(info);
        }
    }
}
