/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.io.IOException;

import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;

public class NewGroupPage extends AbstractGroupPage {

    public NewGroupPage(String userGroupServiceName) {
        super(userGroupServiceName,new GeoServerUserGroup(""));
                
        if (!hasUserGroupStore(userGroupServiceName)) {
            throw new IllegalStateException("New group not possible for read only service");
        }
    }

    @Override
    protected void onFormSubmit(GeoServerUserGroup group) throws IOException {
        GeoServerUserGroupStore store=null;
        try {
            store = new UserGroupStoreValidationWrapper(getUserGroupStore(userGroupServiceName));
            group = store.createGroupObject(group.getGroupname(),group.isEnabled());
            store.addGroup(group);
            store.store();
        } catch (IOException ex) {
            try {
                store.load(); 
            } catch (IOException ex2) {};
            throw ex;
        }

        GeoServerRoleStore gaStore=null;
        try {
            if (hasRoleStore(getSecurityManager().getActiveRoleService().getName())) {
                gaStore = getRoleStore(getSecurityManager().getActiveRoleService().getName());
                gaStore = new RoleStoreValidationWrapper(gaStore);

                for (GeoServerRole role : rolePalette.getSelectedRoles()) {
                    gaStore.associateRoleToGroup(role, group.getGroupname());
                }
                gaStore.store();
            }
        } catch (IOException ex) {
            try {gaStore.load(); } catch (IOException ex2) {};
            throw ex;
        }
    }

}
