/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

public class CatalogFactoryImpl implements CatalogFactory {

    CatalogImpl catalog;
    
    public CatalogFactoryImpl( CatalogImpl catalog ) {
        this.catalog = catalog;
    }
    
    public CoverageInfo createCoverage() {
        return new CoverageInfoImpl(catalog);
    }

    public CoverageDimensionInfo createCoverageDimension() {
        return new CoverageDimensionImpl();
    }

    public CoverageStoreInfo createCoverageStore() {
        return new CoverageStoreInfoImpl(catalog);
    }

    public DataStoreInfo createDataStore() {
        return new DataStoreInfoImpl(catalog);
    }
    
    public WMSStoreInfo createWebMapServer() {
        return new WMSStoreInfoImpl(catalog);
    }
    
    public AttributeTypeInfo createAttribute() {
        return new AttributeTypeInfoImpl();
    }

    public FeatureTypeInfo createFeatureType() {
        return new FeatureTypeInfoImpl(catalog);
    }
    
    public WMSLayerInfo createWMSLayer() {
        return new WMSLayerInfoImpl(catalog);
    }

    public AttributionInfo createAttribution() {
        return new AttributionInfoImpl();
    }
    
    public LayerInfo createLayer() {
        return new LayerInfoImpl();
    }
    
    public MapInfo createMap() {
        return new MapInfoImpl();
    }
    
    public LayerGroupInfo createLayerGroup() {
        return new LayerGroupInfoImpl();
    }
    
    public LegendInfo createLegend() {
        return new LegendInfoImpl();
    }
    
    public MetadataLinkInfo createMetadataLink() {
        return new MetadataLinkInfoImpl();
    }

    public NamespaceInfo createNamespace() {
        return new NamespaceInfoImpl();
    }
    
    public WorkspaceInfo createWorkspace() {
        return new WorkspaceInfoImpl();
    }

    public StyleInfo createStyle() {
        return new StyleInfoImpl(catalog);
    }

    public Object create(Class clazz) {
        return null;
    }
}
