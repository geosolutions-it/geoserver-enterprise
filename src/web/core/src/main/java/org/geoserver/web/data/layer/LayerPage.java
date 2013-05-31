/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.web.data.layer.LayerProvider.ENABLED;
import static org.geoserver.web.data.layer.LayerProvider.NAME;
import static org.geoserver.web.data.layer.LayerProvider.SRS;
import static org.geoserver.web.data.layer.LayerProvider.STORE;
import static org.geoserver.web.data.layer.LayerProvider.TYPE;
import static org.geoserver.web.data.layer.LayerProvider.WORKSPACE;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Page listing all the available layers. Follows the usual filter/sort/page approach,
 * provides ways to bulk delete layers and to add new ones
 */
@SuppressWarnings("serial")
public class LayerPage extends GeoServerSecuredPage {
    LayerProvider provider = new LayerProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table = new GeoServerTablePanel<LayerInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<LayerInfo> property) {
                if(property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                    f.add(new Image("layerIcon", icons.getSpecificLayerIcon((LayerInfo) itemModel.getObject())));
                    return f;
                } else if(property == WORKSPACE) {
                    return workspaceLink(id, itemModel);
                } else if(property == STORE) {
                    return storeLink(id, itemModel);
                } else if(property == NAME) {
                    return layerLink(id, itemModel);
                } else if(property == ENABLED) {
                    LayerInfo layerInfo = (LayerInfo) itemModel.getObject();
                    // ask for enabled() instead of isEnabled() to account for disabled resource/store
                    boolean enabled = layerInfo.enabled();
                    ResourceReference icon = enabled? icons.getEnabledIcon() : icons.getDisabledIcon();
                    Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                    f.add(new Image("layerIcon", icon));
                    return f;
                } else if(property == SRS) {
                    return new Label(id, SRS.getModel(itemModel));
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }
            
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
            }  
            
        };
        table.setOutputMarkupId(true);
        add(table);
        
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        
        // the add button
        header.add(new BookmarkablePageLink("addNew", NewLayerPage.class));
        
        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        
        return header;
    }

    private Component layerLink(String id, final IModel model) {
        IModel layerNameModel = NAME.getModel(model);
        String wsName = (String) WORKSPACE.getModel(model).getObject();
        String layerName = (String) layerNameModel.getObject();
        return new SimpleBookmarkableLink(id, ResourceConfigurationPage.class, layerNameModel, 
                ResourceConfigurationPage.NAME, layerName, ResourceConfigurationPage.WORKSPACE, wsName);
    }

    private Component storeLink(String id, final IModel model) {
        IModel storeModel = STORE.getModel(model);
        String wsName = (String) WORKSPACE.getModel(model).getObject();
        String storeName = (String) storeModel.getObject();
        StoreInfo store = getCatalog().getStoreByName(wsName, storeName, StoreInfo.class);
        if(store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(id, DataAccessEditPage.class, storeModel, 
                    DataAccessEditPage.STORE_NAME, storeName, 
                    DataAccessEditPage.WS_NAME, wsName);
        } else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(id, WMSStoreEditPage.class, storeModel, 
                    DataAccessEditPage.STORE_NAME, storeName, 
                    DataAccessEditPage.WS_NAME, wsName);
        } else {
            return new SimpleBookmarkableLink(id, CoverageStoreEditPage.class, storeModel, 
                    DataAccessEditPage.STORE_NAME, storeName, 
                    DataAccessEditPage.WS_NAME, wsName);
        }
    }

    private Component workspaceLink(String id, final IModel model) {
    	IModel nameModel = WORKSPACE.getModel(model);
        return new SimpleBookmarkableLink(id, WorkspaceEditPage.class, nameModel,
                "name", (String) nameModel.getObject());
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
