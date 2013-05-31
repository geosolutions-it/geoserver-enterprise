/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Helper class for querying the 
 * role hierarchy
 * 
 * @author mcr
 *
 */
public class RoleHierarchyHelper {
    
    Map<String,String> parentMappings;
    
    public RoleHierarchyHelper(Map<String,String> parentMappings) {
        this.parentMappings=parentMappings;
    }

    /**
     * Test if roleName is known
     * 
     * @param roleName
     * @return
     */
    public boolean containsRole(String roleName) {
        return parentMappings.containsKey(roleName);
    }
    
    /**
     * return the parent role name, null if
     * role has no parent
     * 
     * @param roleName
     * @return
     */
    public String getParent(String roleName) {
        checkRole(roleName);
        String parentRole = parentMappings.get(roleName);
        if (roleName.equals(parentRole))
            cycleDetected(roleName, null);
        return parentRole;
    }
    
    /**
     * Calculate an ordered list of ancestors,
     * starting with the parent
     * 
     * @param roleName
     * @return
     */
    public List<String> getAncestors(String roleName) {
        checkRole(roleName);
        List<String> ancestors = new ArrayList<String>();
        fillAncestors(parentMappings.get(roleName), ancestors);
        return ancestors;
    }
    
    /**
     * recursive method to fill the ancestor list
     * 
     * @param roleName
     * @param ancestors
     */
    protected void fillAncestors(String roleName,List<String> ancestors) {
        if (roleName == null || roleName.length()==0)
            return; // end recursion
        ancestors.add(roleName);
        String parentName=parentMappings.get(roleName);
        if (ancestors.contains(parentName)) {
            cycleDetected(roleName, parentName);             
        }
        fillAncestors(parentMappings.get(roleName), ancestors);
    }
    
    /**
     * Return child roles
     * 
     * @param roleName
     * @return
     */
    public List<String> getChildren(String roleName) {
        checkRole(roleName);
        List<String> children = new ArrayList<String>();
        for (Entry<String,String> entry :parentMappings.entrySet()) {
            if (entry.getValue()!= null && entry.getValue().equals(roleName)) {
                if (roleName.equals(entry.getKey()))
                    cycleDetected(roleName,null);
                children.add(entry.getKey());
            }
        }
        return children;        
    }
    
    /**
     * Get all descendant roles, the order is
     * randomly
     * 
     * @param roleName
     * @return
     */
    public List<String> getDescendants(String roleName) {
        checkRole(roleName);
        Set<String> descendants = new HashSet<String>();
        fillDescendents(getChildren(roleName), descendants);
        
        List<String> result=new ArrayList<String>();
        result.addAll(descendants);
        return result;
    }
    
    /**
     * recursive method to fill the descendant list
     * 
     * @param children
     * @param descendants
     */
    protected void fillDescendents(List<String> children,Set<String> descendants) {
        if ( children == null || children.size()==0)
            return; // end recursion
        for (String childName : children) {
            if (descendants.contains(childName)) // cycle
                cycleDetected(childName, null);
            descendants.add(childName);
        }
                
        for (String childName: children) {
            List<String> grandchildren = getChildren(childName);
            fillDescendents(grandchildren, descendants);
        }
    }

    
    /**
     * throws a {@link RuntimeException} for a 
     * non existing role.
     * 
     * @param roleName
     */
    protected void checkRole(String roleName) {
        if (parentMappings.containsKey(roleName)==false)
            throw new RuntimeException("Not extistend role: "+roleName);
    }
    
    /**
     * Throws a {@link RuntimeException} due to
     * a cyclic parent relationship between the two roles 
     * 
     * @param roleName1
     * @param roleName2
     */
    protected void cycleDetected(String roleName1,String roleName2) {
        if (roleName2==null)
            throw new RuntimeException("Cycle detected for "+roleName1);
        else
            throw new RuntimeException("Cycle detected between "+roleName1+
                " and " + roleName2);
    }

    /**
     * Check if the role is a root role
     * 
     * @param roleName
     * @return
     */
    public boolean isRoot(String roleName) {
        checkRole(roleName);
        return parentMappings.get(roleName)==null;
    }
    
    /**
     * Get a list of root roles
     * 
     * @return
     */
    public List<String> getRootRoles() {
        List<String> result = new ArrayList<String>();
        
        for ( String roleName : parentMappings.keySet() ) {
            if (isRoot(roleName))
                result.add(roleName);
        }
        return result;
    }
    
    /**
     * get a list of leaf roles
     * 
     * @return
     */
    public List<String> getLeafRoles() {
        List<String> result = new ArrayList<String>();
        
        Set<String> leafRoles = new HashSet<String>();
        leafRoles.addAll(parentMappings.keySet());        
        for ( String parentRoleName :  parentMappings.values()) {
            if (parentRoleName!=null)
                leafRoles.remove(parentRoleName);
        }
        result.addAll(leafRoles);
        return result;
    }

    /**
     * returns true if parentName is a valid
     * parent for roleName (avoiding cycles)
     * 
     * @param roleName
     * @param parentName
     * @return
     */
    public boolean isValidParent(String roleName,String parentName) {
        if (parentName==null || parentName.length()==0)
            return true;
        if (roleName.equals(parentName))
            return false;
        if (getDescendants(roleName).contains(parentName))
            return false;
        return true;
    }

}
