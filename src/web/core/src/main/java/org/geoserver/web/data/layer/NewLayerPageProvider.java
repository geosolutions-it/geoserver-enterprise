/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.ows.Layer;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * Provides a list of resources for a specific data store
 * @author Andrea Aime - OpenGeo
 *
 */
@SuppressWarnings("serial")
public class NewLayerPageProvider extends GeoServerDataProvider<Resource> {
    
    public static final Property<Resource> PUBLISHED = new BeanProperty<Resource>("published", "published");
    public static final Property<Resource> NAME = new BeanProperty<Resource>("name", "localName");
    public static final Property<Resource> ACTION = new PropertyPlaceholder<Resource>("action");
    
    
    public static final List<Property<Resource>> PROPERTIES = Arrays.asList(PUBLISHED, NAME, ACTION);
    
    boolean showPublished;
    
    String storeId;
    
    transient List<Resource> cachedItems;
    
    @Override
    protected List<Resource> getItems() {
        // return an empty list in case we still don't know about the store
        if(storeId == null) {
            return new ArrayList<Resource>();
        } else if(cachedItems == null) {
            cachedItems = getItemsInternal();
        }
        return cachedItems;
    }

    private List<Resource> getItemsInternal() {
        // else, grab the resource list
        try {
            List<Resource> result;
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);
            
            Map<String, Resource> resources = new HashMap<String, Resource>();
            if(store instanceof DataStoreInfo) {
                DataStoreInfo dstore = (DataStoreInfo) store;
                
                // collect all the type names and turn them into resources
                // for the moment we use local names as datastores are not returning
                // namespace qualified NameImpl
                List<Name> names = dstore.getDataStore(null).getNames();
                for (Name name : names) {
                    Catalog catalog = GeoServerApplication.get().getCatalog();
                    FeatureTypeInfo fti = catalog.getFeatureTypeByDataStore(dstore, name.getLocalPart());
                    // skip views, we cannot have two layers use the same feature type info, as the
                    // underlying definition is attached to the feature type info itself
                    if(fti == null || fti.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE) == null) {
                        resources.put(name.getLocalPart(), new Resource(name));
                    }
                }
                
            } else if(store instanceof CoverageStoreInfo) {
                // getting to the coverage name without reading the whole coverage seems to
                // be hard stuff, let's have the catalog builder to the heavy lifting
                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                CoverageInfo ci = builder.buildCoverage();
                Name name = ci.getQualifiedName();
                resources.put(name.getLocalPart(), new Resource(name));
            } else if(store instanceof WMSStoreInfo) {
                WMSStoreInfo wmsInfo = (WMSStoreInfo) store;
                
                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                List<Layer> layers = wmsInfo.getWebMapServer(null).getCapabilities().getLayerList();
                for(Layer l : layers) {
                    if(l.getName() == null) {
                        continue;
                    }
                    
                    resources.put(l.getName(), new Resource(new NameImpl(l.getName())));
                }
            }
            
            // lookup all configured layers, mark them as published in the resources
            List<ResourceInfo> configuredTypes = getCatalog().getResourcesByStore(store, ResourceInfo.class);
            for (ResourceInfo type : configuredTypes) {
                // compare with native name, which is what the DataStore provides through getNames()
                // above
                Resource resource = resources.get(type.getNativeName());
                if(resource != null)
                    resource.setPublished(true);
            }
            result = new ArrayList<Resource>(resources.values());
            
            // return by natural order
            Collections.sort(result);
            return result;
        } catch(Exception e) {
            throw new RuntimeException("Could not list layers for this store, "
                    + "an error occurred retrieving them: " + e.getMessage(), e);
        }
    }
    
    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.cachedItems = null;
        this.storeId = storeId;
    }

    @Override
    protected List<Resource> getFilteredItems() {
        List<Resource> resources = super.getFilteredItems();
        if(showPublished)
            return resources;
        
        List<Resource> unconfigured = new ArrayList<Resource>();
        for (Resource resource : resources) {
            if(!resource.isPublished())
                unconfigured.add(resource);
        }
        return unconfigured;
    }

    @Override
    protected List<Property<Resource>> getProperties() {
        return PROPERTIES;
    }

    public void setShowPublished(boolean showPublished) {
        this.showPublished = showPublished;
    }

    
}
