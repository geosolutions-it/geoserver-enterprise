/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.importer.WMSLayerImporterPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.data.DataAccess;
import org.geotools.data.wms.WebMapServer;
import org.geotools.jdbc.JDBCDataStore;

/**
 * A page listing the resources contained in a store, and whose links will bring
 * the user to a new resource configuration page
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class NewLayerPage extends GeoServerSecuredPage {

    String storeId;
    private NewLayerPageProvider provider;
    private GeoServerTablePanel<Resource> layers;
    private WebMarkupContainer selectLayersContainer;
    private WebMarkupContainer selectLayers;
    private Label storeName;
    private WebMarkupContainer createTypeContainer;
    private WebMarkupContainer createSQLViewContainer;
    private WebMarkupContainer createWMSLayerImportContainer;
    
    public NewLayerPage() {
        this(null);
    }

    public NewLayerPage(String storeId) {
        this.storeId = storeId;
        
        // the store selector, used when no store is initially known
        Form selector = new Form("selector");
        selector.add(storesDropDown());
        selector.setVisible(storeId == null);
        add(selector);
        
        // the layer choosing block 
        // visible when in any  way a store has been chosen
        selectLayersContainer = new WebMarkupContainer("selectLayersContainer");
        selectLayersContainer.setOutputMarkupId(true);
        add(selectLayersContainer);
        selectLayers = new WebMarkupContainer("selectLayers");
        selectLayers.setVisible(storeId != null);
        selectLayersContainer.add(selectLayers);
        
        selectLayers.add(storeName = new Label("storeName", new Model()));
        if(storeId != null) {
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);
            storeName.setDefaultModelObject(store.getName());
        }
        
        provider = new NewLayerPageProvider();
        provider.setStoreId(storeId);
        provider.setShowPublished(true);
        layers = new GeoServerTablePanel<Resource>("layers", provider) {

            @Override
            protected Component getComponentForProperty(String id,
                    IModel itemModel, Property<Resource> property) {
                if (property == NewLayerPageProvider.NAME) {
                    return new Label(id, property.getModel(itemModel));
                } else if (property == NewLayerPageProvider.PUBLISHED) {
                    final Resource resource = (Resource) itemModel.getObject();
                    final CatalogIconFactory icons = CatalogIconFactory.get();
                    if(resource.isPublished()) {
                        ResourceReference icon = icons.getEnabledIcon();
                        Fragment f = new Fragment(id, "iconFragment", NewLayerPage.this);
                        f.add(new Image("layerIcon", icon));
                        return f;
                    } else {
                        return new Label(id);
                    }
                } else if(property == NewLayerPageProvider.ACTION) {
                    final Resource resource = (Resource) itemModel.getObject();
                    if(resource.isPublished()) {
                        return resourceChooserLink(id, itemModel, new ParamResourceModel("publishAgain", this));
                    } else {
                        return resourceChooserLink(id, itemModel, new ParamResourceModel("publish", this));
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Don't know of property " + property.getName());
                }
            }

        };
        layers.setFilterVisible(true);
        
        selectLayers.add(layers);
        
        createTypeContainer = new WebMarkupContainer("createTypeContainer");
        createTypeContainer.setVisible(false);
        createTypeContainer.add(newFeatureTypeLink());
        selectLayersContainer.add(createTypeContainer);
        
        createSQLViewContainer = new WebMarkupContainer("createSQLViewContainer");
        createSQLViewContainer.setVisible(false);
        createSQLViewContainer.add(newSQLViewLink());
        selectLayersContainer.add(createSQLViewContainer);
        
        createWMSLayerImportContainer = new WebMarkupContainer("createWMSLayerImportContainer");
        createWMSLayerImportContainer.setVisible(false);
        createWMSLayerImportContainer.add(newWMSImportLink());
        selectLayersContainer.add(createWMSLayerImportContainer);
        
        // case where the store is selected, or we have just created new one
        if(storeId != null) {
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);
            updateSpecialFunctionPanels(store);
        }
    }
    
    Component newFeatureTypeLink() {
        return new AjaxLink("createFeatureType") {
            
            @Override
            public void onClick(AjaxRequestTarget target) {
                DataStoreInfo ds = getCatalog().getStore(storeId, DataStoreInfo.class);
                PageParameters pp = new PageParameters("wsName=" + ds.getWorkspace().getName() + ",storeName=" + ds.getName());
                setResponsePage(NewFeatureTypePage.class, pp);                
            }
        };
    }
    
    Component newSQLViewLink() {
        return new AjaxLink("createSQLView") {
            
            @Override
            public void onClick(AjaxRequestTarget target) {
                DataStoreInfo ds = getCatalog().getStore(storeId, DataStoreInfo.class);
                PageParameters pp = new PageParameters("wsName=" + ds.getWorkspace().getName() + ",storeName=" + ds.getName());
                setResponsePage(SQLViewNewPage.class, pp);
            }
        };
    }
    
    Component newWMSImportLink() {
        return new AjaxLink("createWMSImport") {
            
            @Override
            public void onClick(AjaxRequestTarget target) {
                WMSStoreInfo wms = getCatalog().getStore(storeId, WMSStoreInfo.class);
                PageParameters pp = new PageParameters("storeId=" + storeId);
                setResponsePage(WMSLayerImporterPage.class, pp);
            }
        };
    }

    private DropDownChoice storesDropDown() {
        final DropDownChoice stores = new DropDownChoice("storesDropDown", new Model(),
                new StoreListModel(), new StoreListChoiceRenderer());
        stores.setOutputMarkupId(true);
        stores.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (stores.getModelObject() != null) {
                    StoreInfo store = (StoreInfo) stores.getModelObject();
                    NewLayerPage.this.storeId = store.getId();
                    provider.setStoreId(store.getId());
                    storeName.setDefaultModelObject(store.getName());
                    selectLayers.setVisible(true);

                    // make sure we can actually list the contents, it may happen
                    // the store is actually unreachable, in that case we
                    // want to display an error message
                    try {
                        provider.getItems();
                    } catch(Exception e) {
                        LOGGER.log(Level.SEVERE, "Error retrieving layers for the specified store", e);
                        error(e.getMessage());
                        selectLayers.setVisible(false);
                    }
                    
                    updateSpecialFunctionPanels(store);
                    
                } else {
                    selectLayers.setVisible(false);
                    createTypeContainer.setVisible(false);
                }
                target.addComponent(selectLayersContainer);
                target.addComponent(feedbackPanel);

            }

        });
        return stores;
    }

    SimpleAjaxLink resourceChooserLink(String id, IModel itemModel, IModel label) {
        return new SimpleAjaxLink(id, itemModel, label) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Resource resource = (Resource) getDefaultModelObject();
                setResponsePage(new ResourceConfigurationPage(
                        buildLayerInfo(resource), true));
            }

        };
    }
    
    void updateSpecialFunctionPanels(StoreInfo store) {
        // at the moment just assume every store can create types
        createTypeContainer.setVisible(store instanceof DataStoreInfo);

        // reset to default first, to avoid the container being displayed if store is not a
        // DataStoreInfo
        createSQLViewContainer.setVisible(false);
        if (store instanceof DataStoreInfo) {
            try {
                DataAccess da = ((DataStoreInfo) store).getDataStore(null);
                createSQLViewContainer.setVisible(da instanceof JDBCDataStore);
            } catch (IOException e) {

            }
        }

        // reset to default first, to avoid the container being displayed if store is not a
        // WMSStoreInfo
        createWMSLayerImportContainer.setVisible(false);
        if (store instanceof WMSStoreInfo) {
            try {
                WebMapServer wms = ((WMSStoreInfo) store).getWebMapServer(null);
                createWMSLayerImportContainer.setVisible(wms != null);
            } catch (IOException e) {

            }
        }
    }

    /**
     * Turns a resource name into a full {@link ResourceInfo}
     * 
     * @param resource
     * @return
     */
    LayerInfo buildLayerInfo(Resource resource) {
        Catalog catalog = getCatalog();
        StoreInfo store = catalog.getStore(getSelectedStoreId(), StoreInfo.class);

        // try to build from coverage store or data store
        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(store);
            if (store instanceof CoverageStoreInfo) {
                CoverageInfo ci = builder.buildCoverage();
                return builder.buildLayer(ci);
            } else if (store instanceof DataStoreInfo) {
                FeatureTypeInfo fti = builder.buildFeatureType(resource.getName());
                return builder.buildLayer(fti);
            } else if (store instanceof WMSStoreInfo) {
                WMSLayerInfo wli = builder.buildWMSLayer(resource.getLocalName());
                return builder.buildLayer(wli);
            } 
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error occurred while building the resources for the configuration page",
                    e);
        }

        // handle the case in which the store was not found anymore, or was not
        // of the expected type
        if (store == null)
            throw new IllegalArgumentException(
                    "Store is missing from configuration!");
        else
            throw new IllegalArgumentException(
                    "Don't know how to deal with this store " + store);
    }
    
    /**
     * Returns the storeId provided during construction, or the one pointed
     * by the drop down if none was provided during construction
     * @return
     */
    String getSelectedStoreId() {
        // the provider is always up to date 
        return provider.getStoreId();
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
