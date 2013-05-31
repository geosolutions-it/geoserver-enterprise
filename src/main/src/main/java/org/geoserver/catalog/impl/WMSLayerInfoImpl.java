/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;

import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.data.ows.Layer;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class WMSLayerInfoImpl extends ResourceInfoImpl implements WMSLayerInfo {

    
    protected WMSLayerInfoImpl() {
    }
    
    public WMSLayerInfoImpl(CatalogImpl catalog) {
        super(catalog);
    }

    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        return catalog.getResourcePool().getWMSLayer(this);
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public WMSStoreInfo getStore() {
        return (WMSStoreInfo) super.getStore();
    }

}
