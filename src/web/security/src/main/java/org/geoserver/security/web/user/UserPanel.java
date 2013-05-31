/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;

/**
 * A page listing users, allowing for removal, addition and linking to an edit page
 */
@SuppressWarnings("serial")
public class UserPanel extends Panel {

    protected GeoServerTablePanel<GeoServerUser> users;
    protected GeoServerDialog dialog;
    protected SelectionUserRemovalLink removal,removalWithRoles;
    protected Link<NewUserPage> add;
    protected String serviceName;

    protected GeoServerUserGroupService getService() {
        try {
            return GeoServerApplication.get().getSecurityManager().
                    loadUserGroupService(serviceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public UserPanel(String id, String serviceName) {
        super(id);
        
        this.serviceName=serviceName;
        UserListProvider provider = new UserListProvider(this.serviceName);
        add(users = new UserTablePanel("table", serviceName, provider, true) {
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(users.getSelection().size() > 0);
                target.addComponent(removal);
                removalWithRoles.setEnabled(users.getSelection().size() > 0);
                target.addComponent(removalWithRoles);
            }
        });
        users.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        headerComponents();
        
    }

    public UserPanel setHeaderVisible(boolean visible) {
        get("header").setVisible(visible);
        return this;
    }

    public UserPanel setPagersVisible(boolean top, boolean bottom) {
        users.getTopPager().setVisible(top);
        users.getBottomPager().setVisible(bottom);
        return this;
    }

    protected void headerComponents() {

        
        boolean canCreateStore=getService().canCreateStore();

        WebMarkupContainer h = new WebMarkupContainer("header");
        add(h);

        if (!canCreateStore) {
            h.add(new Label("message", new StringResourceModel("noCreateStore", this, null))
                .add(new AttributeAppender("class", new Model("info-link"), " ")));
        }
        else {
            h.add(new Label("message", new Model())
                .add(new AttributeAppender("class", new Model("displayNone"), " ")));
        }

        // the add button
        h.add(add=new Link("addNew") {
            @Override
            public void onClick() {
                setResponsePage(new NewUserPage(serviceName).setReturnPage(this.getPage()));
            }
        });
        
        //<NewUserPage><NewUserPage>("addNew", NewUserPage.class));
        //add.setParameter(AbstractSecurityPage.ServiceNameKey, serviceName);
        add.setVisible(canCreateStore);

        // the removal button
        h.add(removal = new SelectionUserRemovalLink(serviceName,"removeSelected", users, dialog,false));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.setVisible(canCreateStore);
        

        h.add(removalWithRoles = new SelectionUserRemovalLink(serviceName,"removeSelectedWithRoles", users, dialog,true));
        removalWithRoles.setOutputMarkupId(true);
        removalWithRoles.setEnabled(false);
        removalWithRoles.setVisible(canCreateStore && 
                GeoServerApplication.get().getSecurityManager().
                    getActiveRoleService().canCreateStore());
        
    }

}
