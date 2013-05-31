/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Implementation for testing
 * uses serialization into a byte array
 * 
 * @author christian
 *
 */
public class MemoryRoleStore extends AbstractRoleStore {
    
    
    

    @Override
    protected void serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(helper.roleMap);
        oout.writeObject(helper.role_parentMap);
        oout.writeObject(helper.user_roleMap);
        oout.writeObject(helper.group_roleMap);
        ((MemoryRoleService)service).byteArray=out.toByteArray();
        oout.close();            
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deserialize() throws IOException {
        clearMaps();
        byte[] bytes = ((MemoryRoleService) service).byteArray;
        if (bytes==null) {
            setModified(false);
            return;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        try {
            helper.roleMap = (TreeMap<String,GeoServerRole>) oin.readObject();
            helper.role_parentMap =(HashMap<GeoServerRole,GeoServerRole>) oin.readObject();
            helper.user_roleMap = (TreeMap<String,SortedSet<GeoServerRole>>)oin.readObject();
            helper.group_roleMap = (TreeMap<String,SortedSet<GeoServerRole>>)oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        setModified(false);
    }
    
    @Override
    public GeoServerRole createRoleObject(String role)
            throws IOException {
        return new MemoryGeoserverRole(role);
    }


}
