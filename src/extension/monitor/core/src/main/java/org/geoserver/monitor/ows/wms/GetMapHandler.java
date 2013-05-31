/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;

public class GetMapHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

   public GetMapHandler(MonitorConfig config) {
        super("org.geoserver.wms.GetMapRequest", config);
    }

    @Override
    public List<String> getLayers(Object request) {
        @SuppressWarnings("unchecked")
        List<Object> mapLayers = (List<Object>) OwsUtils.get(request, "layers");
        if (mapLayers == null) {
            return null;
        }
        
        List<String> layers = new ArrayList<String>();
        for (int i = 0; i < mapLayers.size(); i++) {
            layers.add((String) OwsUtils.get(mapLayers.get(i), "name"));
        }
        
        return layers;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) OwsUtils.get(request, "crs");
        Envelope env = (Envelope) OwsUtils.get(request, "bbox");
        BoundingBox bbox = new ReferencedEnvelope(env, crs);
        
        try{
            return bbox.toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
    }
}
