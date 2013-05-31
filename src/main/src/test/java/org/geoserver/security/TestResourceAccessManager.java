package org.geoserver.security;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.security.core.Authentication;

/**
 * A simple {@link ResourceAccessManager} that keeps all the limits in a in memory hash map. 
 * Useful for testing purposes
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class TestResourceAccessManager implements ResourceAccessManager {
    
    Map<String, Map<String, AccessLimits>> limits = new HashMap<String, Map<String,AccessLimits>>();  

    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if(user == null) {
            return null;
        }
        
        final String name = user.getName();
        return (WorkspaceAccessLimits) getUserMap(name).get(workspace.getId());
    }

    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        if(user == null) {
            return null;
        }
        
        final String name = user.getName();
        DataAccessLimits limits = (DataAccessLimits) getUserMap(name).get(layer.getId());
        if(limits == null) {
            limits = getAccessLimits(user, layer.getResource());
        }
        return limits;
    }

    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if(user == null) {
            return null;
        }
        
        final String name = user.getName();
        return (DataAccessLimits) getUserMap(name).get(resource.getId());
    }

    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        if(user == null) {
            return null;
        }
        
        final String name = user.getName();
        return (StyleAccessLimits) getUserMap(name).get(style.getId());
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        if(user == null) {
            return null;
        }
        
        final String name = user.getName();
        return (LayerGroupAccessLimits) getUserMap(name).get(layerGroup.getId());
    }
    
    /**
     * Saves the mock access limits for this user and secured item (this is meant only for testing,
     * it's the caller care to make sure the appropriate user limits class is used).
     * The CatalogInfo is required to have a valid and stable id.
     * @param userName
     * @param securedItem
     * @param limits
     */
    public void putLimits(String userName, CatalogInfo securedItem, AccessLimits limits) {
        getUserMap(userName).put(securedItem.getId(), limits);
    }
    
    Map<String, AccessLimits> getUserMap(String userName) {
        Map<String, AccessLimits> userMap = limits.get(userName);
        if(userMap == null) {
            userMap = new HashMap<String, AccessLimits>();
            limits.put(userName, userMap);
        }
        return userMap;
    }
}
