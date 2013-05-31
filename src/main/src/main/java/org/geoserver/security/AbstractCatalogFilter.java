/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * A convenient base class for catalog filters. By default does not filter anything, it is advised
 * to use this class as the base to protect yourself from CatalogFilter API changes, implement
 * CatalogFilter directly only if you need a different base class
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public abstract class AbstractCatalogFilter implements CatalogFilter {

    @Override
    public boolean hideLayer(LayerInfo layer) {
        return false;
    }

    @Override
    public boolean hideStyle(StyleInfo style) {
        return false;
    }

    @Override
    public boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        return false;
    }

    @Override
    public boolean hideWorkspace(WorkspaceInfo workspace) {
        return false;
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        return false;
    }

}
