/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.security.core.Authentication;

/**
 * Abstract base class for {@link ResourceAccessManager} implementations.
 * <p>
 * This base class returns null from every method meaning no limits.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AbstractResourceAccessManager implements ResourceAccessManager {

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user,
            WorkspaceInfo workspace) {
        return null;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return null;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        return null;
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return null;
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return null;
    }

}
