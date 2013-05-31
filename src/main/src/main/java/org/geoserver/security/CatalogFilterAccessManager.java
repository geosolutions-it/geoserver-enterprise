/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Filters viewable layers based on the registered CatalogFilter
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author David Winslow, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class CatalogFilterAccessManager extends ResourceAccessManagerWrapper {

    private List<? extends CatalogFilter> filters;

    private DataAccessLimits hide(ResourceInfo info) {
        if (info instanceof FeatureTypeInfo) {
            return new VectorAccessLimits(CatalogMode.HIDE, null, Filter.EXCLUDE, null,
                    Filter.EXCLUDE);
        } else if (info instanceof CoverageInfo) {
            return new CoverageAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, null);
        } else if (info instanceof WMSLayerInfo) {
            return new WMSAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, false);
        } else {
            // TODO: Log warning about unknown resource type
            return new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE);
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        if (hideLayer(layer) || hideResource(layer.getResource())) {
            return hide(layer.getResource());
        }
        return super.getAccessLimits(user, layer);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (hideResource(resource)) {
            return hide(resource);
        } else {
            return super.getAccessLimits(user, resource);
        }
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if (hideWorkspace(workspace)) {
            return new WorkspaceAccessLimits(CatalogMode.HIDE, false, false, false);
        } else {
            return super.getAccessLimits(user, workspace);
        }
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        if (hideStyle(style)) {
            return new StyleAccessLimits(CatalogMode.HIDE);
        }
        else {
            return super.getAccessLimits(user, style);
        }
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        if (hideLayerGroup(layerGroup)) {
            return new LayerGroupAccessLimits(CatalogMode.HIDE);
        }
        else {
            return super.getAccessLimits(user, layerGroup);
        }
    }
    
    private boolean hideResource(ResourceInfo resource) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideResource(resource)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayer(LayerInfo layer) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayer(layer)) {
                return true;
            }
        }
        return false;

    }

    private boolean hideWorkspace(WorkspaceInfo workspace) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideWorkspace(workspace)) {
                return true;
            }
        }
        return false;

    }

    private boolean hideStyle(StyleInfo style) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideStyle(style)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayerGroup(layerGroup)) {
                return true;
            }
        }
        return false;
    }
    
    private List<? extends CatalogFilter> getCatalogFilters() {
        if (filters == null) {
            filters = GeoServerExtensions.extensions(CatalogFilter.class);
        }
        return filters;
    }
    
    /**
     * Designed for testing, allows to manually configure the catalog filters bypassing
     * the Spring context lookup
     * @param filters
     */
    public void setCatalogFilters(List<? extends CatalogFilter> filters) {
        this.filters = filters;
    }

}
