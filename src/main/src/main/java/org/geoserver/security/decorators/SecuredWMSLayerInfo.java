/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.ows.Layer;
import org.opengis.util.ProgressListener;

/**
 * Wraps a {@link WMSLayerInfo} so that it will return secured layers and WMS stores
 * 
 * @author Andrea Aime - TOPP
 */
public class SecuredWMSLayerInfo extends DecoratingWMSLayerInfo {

    WrapperPolicy policy;

    public SecuredWMSLayerInfo(WMSLayerInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }
    
    @Override
    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        Layer layer = super.getWMSLayer(listener);
        if(layer == null) {
            return layer;
        } else {
            return new SecuredWMSLayer(layer, policy);
        }
    }

    @Override
    public WMSStoreInfo getStore() {
        return new SecuredWMSStoreInfo(delegate.getStore(), policy);
    }
}
