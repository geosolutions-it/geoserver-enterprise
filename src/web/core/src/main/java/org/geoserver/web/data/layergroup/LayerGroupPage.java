/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Lists layer groups, allows removal and editing
 */
@SuppressWarnings("serial")
public class LayerGroupPage extends GeoServerSecuredPage {
    
    GeoServerTablePanel<LayerGroupInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerGroupPage() {
        LayerGroupProvider provider = new LayerGroupProvider();
        add(table = new GeoServerTablePanel<LayerGroupInfo>( "table", provider, true ) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<LayerGroupInfo> property) {
                
                if ( property == LayerGroupProvider.NAME ) {
                    return layerGroupLink( id, itemModel ); 
                }
                if (property == LayerGroupProvider.WORKSPACE) {
                    return workspaceLink(id, itemModel);
                }
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                if (!table.getSelection().isEmpty()) {
                    boolean canRemove = true;
                    if (!isAuthenticatedAsAdmin()) {
                        //if any global layer groups are selected, don't allow delete
                        for (LayerGroupInfo lg : table.getSelection()) {
                            if (lg.getWorkspace() == null) {
                                canRemove = false;
                                break;
                            }
                        }
                    }

                    removal.setEnabled(canRemove);
                }
                else {
                    removal.setEnabled(false);
                }
                target.addComponent(removal);
            }  
        });
        table.setOutputMarkupId(true);
        add(table);
        
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        
        // the add button
        header.add(new BookmarkablePageLink("addNew", LayerGroupNewPage.class));
        
        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        
        return header;
    }
    
    Component layerGroupLink(String id, IModel itemModel) {
        IModel groupNameModel = LayerGroupProvider.NAME.getModel(itemModel);
        IModel wsModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);
        
        String groupName = (String) groupNameModel.getObject();
        String wsName = (String) wsModel.getObject();
        
        return new SimpleBookmarkableLink(id, LayerGroupEditPage.class, groupNameModel, 
            LayerGroupEditPage.GROUP, groupName, LayerGroupEditPage.WORKSPACE, wsName);
    }
   

    Component workspaceLink(String id, IModel itemModel) {
        IModel wsNameModel = LayerGroupProvider.WORKSPACE.getModel(itemModel);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(
                id, WorkspaceEditPage.class, new Model(wsName), "name", wsName);
        }
        else {
            return new WebMarkupContainer(id);
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
