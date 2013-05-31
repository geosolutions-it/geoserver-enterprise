/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.opengis.feature.type.FeatureType;

public class GetLegendGraphicHandler extends RequestObjectHandler {

    public GetLegendGraphicHandler(MonitorConfig config) {
        super("org.geoserver.wms.GetLegendGraphicRequest", config);
    }

    @Override
    protected List<String> getLayers(Object request) {
        List<FeatureType> types = (List<FeatureType>) OwsUtils.get(request,
                "layers");
    
        if (types != null) {
            List<String> result = new ArrayList<String>();
            for (FeatureType ft : types) {
                result.add(ft.getName().toString());
            }
            return result;
        }
        return null;
    }

}
