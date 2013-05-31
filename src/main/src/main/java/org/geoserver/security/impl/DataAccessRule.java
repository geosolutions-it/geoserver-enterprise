/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geoserver.security.AccessMode;

/**
 * Represents a data access rule: identifies a workspace, a layer, an access mode, and the set of
 * roles that are allowed to access it
 * <p>Mind, two rules are considered equal if the address the same data, if you need full
 * comparison, use {@link #equalsExact(DataAccessRule)}</p>
 */
@SuppressWarnings("serial")
public class DataAccessRule implements Comparable<DataAccessRule>, Serializable {

    /**
     * Any layer, or any workspace, or any role
     */
    public static final String ANY = "*";
    public static DataAccessRule READ_ALL = new DataAccessRule(ANY, ANY, AccessMode.READ);
    public static DataAccessRule WRITE_ALL = new DataAccessRule(ANY, ANY, AccessMode.WRITE);

    String workspace;

    String layer;

    AccessMode accessMode;

    Set<String> roles;

    /**
     * Builds a new rule
     */
    public DataAccessRule(String workspace, String layer, AccessMode accessMode, Set<String> roles) {
        super();
        this.workspace = workspace;
        this.layer = layer;
        this.accessMode = accessMode;
        if (roles == null)
            this.roles = new HashSet<String>();
        else
            this.roles = new HashSet<String>(roles);
    }
    
    /**
     * Builds a new rule
     */
    public DataAccessRule(String workspace, String layer, AccessMode accessMode, String... roles) {
        this(workspace, layer, accessMode, roles == null ? null : new HashSet<String>(Arrays.asList(roles)));
    }

    /**
     * Copy constructor
     */
    public DataAccessRule(DataAccessRule other) {
        this.workspace = other.workspace;
        this.layer = other.layer;
        this.accessMode = other.accessMode;
        this.roles = new HashSet<String>(other.roles);
    }

    /**
     * Builds the default rule: *.*.r=*
     */
    public DataAccessRule() {
        this(ANY, ANY, AccessMode.READ);
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Returns the key for the current rule. No other rule should have the same
     * 
     * @return
     */
    public String getKey() {
        return workspace + "." + layer + "." + accessMode.getAlias();
    }
    
    /**
     * Returns the list of roles as a comma separated string for this rule
     * @return
     */
    public String getValue() {
        if(roles.isEmpty()) {
            return DataAccessRule.ANY;
        } else {
            StringBuffer sb = new StringBuffer();
            for (String role : roles) {
                sb.append(role);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        } 
    }

    /**
     * Comparison implemented so that generic rules get first, specific one are compared by name,
     * and if anything else is equal, read comes before write
     */
    public int compareTo(DataAccessRule other) {
        int compareWs = compareCatalogItems(workspace, other.workspace);
        if (compareWs != 0)
            return compareWs;

        int compareLayer = compareCatalogItems(layer, other.layer);
        if (compareLayer != 0)
            return compareLayer;

        if (accessMode.equals(other.accessMode))
            return 0;
        else
            return accessMode.equals(AccessMode.READ) ? -1 : 1;
    }

    /**
     * Equality based on ws/layer/mode only
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataAccessRule))
            return false;

        return 0 == compareTo((DataAccessRule) obj);
    }
    
    /**
     * Full equality, roles included
     */
    public boolean equalsExact(DataAccessRule obj) {
        if(0 != compareTo(obj))
            return false;
        else
            return roles.equals(obj.roles);
    }

    /**
     * Hashcode based on wfs/layer/mode only
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(workspace).append(layer).append(accessMode.getAlias())
                .toHashCode();
    }

    /**
     * Generic string comparison that considers the use of {@link #ANY}
     */
    public int compareCatalogItems(String item, String otherItem) {
        if (item.equals(otherItem))
            return 0;
        else if (ANY.equals(item))
            return -1;
        else if (ANY.equals(otherItem))
            return 1;
        else
            return item.compareTo(otherItem);

    }
    
    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
