/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.web.SecurityNamedServicesTogglePanel;
import org.geoserver.security.web.group.GroupPanel;
import org.geoserver.security.web.user.UserPanel;

/**
 * Toggle panel for user group services.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupServicesTogglePanel 
    extends SecurityNamedServicesTogglePanel<SecurityUserGroupServiceConfig> {

    public UserGroupServicesTogglePanel(String id) {
        super(id, new UserGroupServiceConfigListModel());
    }

    @Override
    protected ContentPanel createPanel(String id, IModel<SecurityUserGroupServiceConfig> model) {
        return new UsersGroupsPanel(id, model);
    }

    static class UsersGroupsPanel extends ContentPanel<SecurityUserGroupServiceConfig> {

        public UsersGroupsPanel(String id, final IModel<SecurityUserGroupServiceConfig> model) {
            super(id, model);

            SecurityUserGroupServiceConfig config = model.getObject();
            add(new UserPanel("users", config.getName())
                .setHeaderVisible(false).setPagersVisible(false, true));
            add(new GroupPanel("groups", config.getName())
                .setHeaderVisible(false).setPagersVisible(false, true));
        }
    }
}
