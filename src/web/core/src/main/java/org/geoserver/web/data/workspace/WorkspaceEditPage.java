/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.admin.ContactPanel;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.data.namespace.NamespaceDetachableModel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.services.ServiceMenuPageInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.logging.Logging;

/**
 * Allows editing a specific workspace
 */
@SuppressWarnings("serial")
public class WorkspaceEditPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.workspace");
    
    IModel wsModel;
    IModel nsModel;
    boolean defaultWs;

    SettingsPanel settingsPanel;
    ServicesPanel servicesPanel;
    GeoServerDialog dialog;
    
    /**
     * Uses a "name" parameter to locate the workspace
     * @param parameters
     */
    public WorkspaceEditPage(PageParameters parameters) {
        String wsName = parameters.getString("name");
        WorkspaceInfo wsi = getCatalog().getWorkspaceByName(wsName);
        
        if(wsi == null) {
            error(new ParamResourceModel("WorkspaceEditPage.notFound", this, wsName).getString());
            doReturn(WorkspacePage.class);
            return;
        }
        
        init(wsi);
    }
    
    public WorkspaceEditPage(WorkspaceInfo ws) {
        init(ws);
    }
    
    private void init(WorkspaceInfo ws) {
        defaultWs = ws.getId().equals(getCatalog().getDefaultWorkspace().getId());
        
        wsModel = new WorkspaceDetachableModel( ws );

        NamespaceInfo ns = getCatalog().getNamespaceByPrefix( ws.getName() );
        nsModel = new NamespaceDetachableModel(ns);
        
        Form form = new Form( "form", new CompoundPropertyModel( nsModel ) ) {
            protected void onSubmit() {
                try {
                    saveWorkspace();
                } catch (RuntimeException e) {
                    error(e.getMessage());
                }
            }
        };
        add(form);

        //check for full admin, we don't allow workspace admins to change all settings
        boolean isFullAdmin = isAuthenticatedAsAdmin();
        
        TextField name = new TextField("name", new PropertyModel(wsModel, "name"));
        name.setRequired(true);
        name.setEnabled(isFullAdmin);

        name.add(new XMLNameValidator());
        form.add(name);
        TextField uri = new TextField("uri", new PropertyModel(nsModel, "uRI"), String.class);
        uri.setRequired(true);
        uri.add(new URIValidator());
        form.add(uri);
        CheckBox defaultChk = new CheckBox("default", new PropertyModel(this, "defaultWs"));
        form.add(defaultChk);
        defaultChk.setEnabled(isFullAdmin);

        //stores
//        StorePanel storePanel = new StorePanel("storeTable", new StoreProvider(ws), false);
//        form.add(storePanel);
        
        add(dialog = new GeoServerDialog("dialog"));

        //local settings
        form.add(settingsPanel = new SettingsPanel("settings", wsModel));
        form.add(new HelpLink("settingsHelp").setDialog(dialog));

        //local services
        form.add(servicesPanel = new ServicesPanel("services", wsModel));
        form.add(new HelpLink("servicesHelp").setDialog(dialog));

        SubmitLink submit = new SubmitLink("save");
        form.add(submit);
        form.setDefaultButton(submit);
        form.add(new BookmarkablePageLink("cancel", WorkspacePage.class));
    }

    private void saveWorkspace() {
        final Catalog catalog = getCatalog();

        NamespaceInfo namespaceInfo = (NamespaceInfo) nsModel.getObject();
        WorkspaceInfo workspaceInfo = (WorkspaceInfo) wsModel.getObject();
        
        // sync up workspace name with namespace prefix, temp measure until the two become separate
        namespaceInfo.setPrefix(workspaceInfo.getName());
        
        // this will ensure all datastore namespaces are updated when the workspace is modified
        catalog.save(workspaceInfo);
        catalog.save(namespaceInfo);
        if(defaultWs) {
            catalog.setDefaultWorkspace(workspaceInfo);
        }

        GeoServer geoServer = getGeoServer();

        //persist/depersist any settings configured local to the workspace
        Settings set = settingsPanel.set;
        if (set.enabled) {
            if (set.model instanceof NewSettingsModel) {
                geoServer.add(set.model.getObject());
            }
            else {
                geoServer.save(set.model.getObject());
            }
        }
        else {
            //remove if necessary
            if (set.model instanceof ExistingSettingsModel) {
                geoServer.remove(set.model.getObject());
            }
        }
        
        //persist/depersist any services configured local to this workspace
        for (Service s : servicesPanel.services) {
            if (s.enabled) {
                if (s.model instanceof ExistingServiceModel) {
                    //nothing to do, service has already been added
                    continue;
                }
                geoServer.add(s.model.getObject());
            }
            else {
                //remove if necessary
                if (s.model instanceof ExistingServiceModel) {
                    //means they are removing an existing service, look it up and remove
                    geoServer.remove(s.model.getObject());
                }
            }
        }
        doReturn(WorkspacePage.class);
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /*
     * Data object to hold onto transient settings, and maintain state of enabled for the workspace.
     */
    static class Settings implements Serializable {
        /** track selection */
        Boolean enabled;

        /** created settings, not yet added to configuration */
        IModel<SettingsInfo> model;
    }

    static class ExistingSettingsModel extends LoadableDetachableModel<SettingsInfo> {

        IModel<WorkspaceInfo> wsModel;

        ExistingSettingsModel(IModel<WorkspaceInfo> wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        protected SettingsInfo load() {
            GeoServer gs = GeoServerApplication.get().getGeoServer();
            return gs.getSettings(wsModel.getObject());
        }

    }

    static class NewSettingsModel extends Model<SettingsInfo> {

        IModel<WorkspaceInfo> wsModel;
        SettingsInfo info;

        NewSettingsModel(IModel<WorkspaceInfo> wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        public SettingsInfo getObject() {
            if (info == null) {
                GeoServer gs = GeoServerApplication.get().getGeoServer();
                info = gs.getFactory().createSettings();
                
                //initialize from global settings
                SettingsInfo global = gs.getGlobal().getSettings();

                //hack, we need to copy out composite objects separately to get around proxying
                // madness
                ContactInfo contact = gs.getFactory().createContact();
                OwsUtils.copy(global.getContact(), contact, ContactInfo.class);

                OwsUtils.copy(global, info, SettingsInfo.class);
                info.setContact(contact);

                info.setWorkspace(wsModel.getObject());
            }
            return info;
        }
    }

    class SettingsPanel extends FormComponentPanel {

        WebMarkupContainer settingsContainer;
        ContactPanel contactPanel;
        WebMarkupContainer otherSettingsPanel;
        Settings set;

        public SettingsPanel(String id, IModel<WorkspaceInfo> model) {
            super(id, new Model());

            SettingsInfo settings = getGeoServer().getSettings(model.getObject());

            set = new Settings();
            set.enabled = settings != null;
            set.model = settings != null ? 
                new ExistingSettingsModel(wsModel) : new NewSettingsModel(wsModel); 

            add(new CheckBox("enabled", new PropertyModel<Boolean>(set, "enabled")).
                add(new AjaxFormComponentUpdatingBehavior("onclick") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        contactPanel.setVisible(set.enabled);
                        otherSettingsPanel.setVisible(set.enabled);
                        target.addComponent(settingsContainer);
                    }
                }));

            settingsContainer = new WebMarkupContainer("settingsContainer");
            settingsContainer.setOutputMarkupId(true);
            add(settingsContainer);

            contactPanel = 
                new ContactPanel("contact", new CompoundPropertyModel<ContactInfo>(
                    new PropertyModel<ContactInfo>(set.model, "contact")));
            contactPanel.setOutputMarkupId(true);
            contactPanel.setVisible(set.enabled);
            settingsContainer.add(contactPanel);

            otherSettingsPanel = new WebMarkupContainer("otherSettings", 
                new CompoundPropertyModel<GeoServerInfo>(set.model));
            otherSettingsPanel.setOutputMarkupId(true);
            otherSettingsPanel.setVisible(set.enabled);
            otherSettingsPanel.add(new CheckBox("verbose"));
            otherSettingsPanel.add(new CheckBox("verboseExceptions"));
            otherSettingsPanel.add(new TextField<Integer>("numDecimals").add(new MinimumValidator<Integer>(0)));
            otherSettingsPanel.add(new DropDownChoice("charset", GlobalSettingsPage.AVAILABLE_CHARSETS));
            otherSettingsPanel.add(new TextField("proxyBaseUrl").add(new UrlValidator()));
            settingsContainer.add(otherSettingsPanel);

        }
    }

    /*
     * Data object to hold onto transient services, and maintain state of selected services for 
     * the workspace.
     */
    static class Service implements Serializable {
        /** track selection */
        Boolean enabled;

        /** the admin page for the service */ 
        ServiceMenuPageInfo adminPage;

        /** created service, not yet added to configuration */
        IModel<ServiceInfo> model;
    }

    static class NewServiceModel extends Model<ServiceInfo> {
        
        IModel<WorkspaceInfo> wsModel;
        Class<ServiceInfo> serviceClass;
        ServiceInfo service;

        NewServiceModel(IModel<WorkspaceInfo> wsModel, Class<ServiceInfo> serviceClass) {
            this.wsModel = wsModel;
            this.serviceClass = serviceClass;
        }

        @Override
        public ServiceInfo getObject() {
            if (service == null) {
                service = create();
            }
            return service;
        }

        ServiceInfo create() {
            //create it
            GeoServer gs = GeoServerApplication.get().getGeoServer();
            
            ServiceInfo newService = gs.getFactory().create(serviceClass);

            //initialize from global service
            ServiceInfo global = gs.getService(serviceClass);
            OwsUtils.copy(global,newService, serviceClass);
            newService.setWorkspace(wsModel.getObject());

            //hack, but need id to be null so its considered unattached
            ((ServiceInfoImpl)newService).setId(null);
            
            return newService;
        }
    }

    static class ExistingServiceModel extends LoadableDetachableModel<ServiceInfo> {

        IModel<WorkspaceInfo> wsModel;
        Class<ServiceInfo> serviceClass;

        ExistingServiceModel(IModel<WorkspaceInfo> wsModel, Class<ServiceInfo> serviceClass) {
            this.wsModel = wsModel;
            this.serviceClass = serviceClass;
        }

        @Override
        protected ServiceInfo load() {
            return GeoServerApplication.get().getGeoServer().getService(wsModel.getObject(), serviceClass);
        }
    }

    class ServicesPanel extends FormComponentPanel {

        List<Service> services;
        
        public ServicesPanel(String id, final IModel<WorkspaceInfo> wsModel) {
            super(id, new Model());

            services = services(wsModel);
            ListView<Service> serviceList = new ListView<Service>("services", services) {

                @Override
                protected void populateItem(ListItem<Service> item) {
                    Service service = item.getModelObject();

                    final Link<Service> link = new Link<Service>("link", new Model(service)) {
                        @Override
                        public void onClick() {
                            Service s = getModelObject();
                            Page page = null;

                            if (s.model instanceof ExistingServiceModel) {
                                //service that has already been added, 
                                PageParameters pp = 
                                        new PageParameters("workspace=" + wsModel.getObject().getName());
                                try {
                                    page = s.adminPage.getComponentClass()
                                        .getConstructor(PageParameters.class).newInstance(pp);
                                } catch (Exception e) {
                                    throw new WicketRuntimeException(e);
                                }
                            }
                            else {
                                //service that has yet to be added
                                try {
                                    page = s.adminPage.getComponentClass().getConstructor(
                                            s.adminPage.getServiceClass()).newInstance(s.model.getObject());
                                }
                                catch (Exception e) {
                                    throw new WicketRuntimeException(e);
                                }
                                
                            }
                            ((BaseServiceAdminPage)page).setReturnPage(WorkspaceEditPage.this);
                            setResponsePage(page);
                        }
                    };
                    link.setOutputMarkupId(true);
                    link.setEnabled(service.enabled);
                    
                    AjaxCheckBox enabled = 
                        new AjaxCheckBox("enabled", new PropertyModel<Boolean>(service, "enabled")) {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            link.setEnabled(getModelObject());
                            target.addComponent(link);
                        }
                    };
                    item.add(enabled);
                    
                    ServiceMenuPageInfo info = service.adminPage;
                    
                    link.add(new AttributeModifier("title", true, 
                        new StringResourceModel(info.getDescriptionKey(), (Component) null, null)));
                    link.add(new Label("link.label", 
                        new StringResourceModel(info.getTitleKey(), (Component) null, null)));
                    
                    Image image;
                    if(info.getIcon() != null) {
                        image = new Image("link.icon", 
                            new ResourceReference(info.getComponentClass(), info.getIcon()));
                    } else {
                        image = new Image("link.icon", 
                            new ResourceReference(GeoServerBasePage.class, "img/icons/silk/wrench.png"));
                    }
                    image.add(new AttributeModifier("alt", true, new ParamResourceModel(info.getTitleKey(), null)));
                    link.add(image);
                    item.add(link);
                }
            };
            add(serviceList);
        }

        List<Service> services(IModel<WorkspaceInfo> wsModel) {
            List<Service> services = new ArrayList();
            
            for (ServiceMenuPageInfo page : 
                    getGeoServerApplication().getBeansOfType(ServiceMenuPageInfo.class)) {
                Service service = new Service();
                service.adminPage = page;
                service.enabled = 
                    getGeoServer().getService(wsModel.getObject(), page.getServiceClass()) != null;

                //if service is disabled, create a placeholder model to hold a newly created one,
                // otherwise create a live model to the existing service
                Class<ServiceInfo> serviceClass = (Class<ServiceInfo>) page.getServiceClass();
                service.model = !service.enabled ? new NewServiceModel(wsModel, serviceClass) :  
                    new ExistingServiceModel(wsModel, serviceClass);
                services.add(service);
            }

            return services;
        }
    }
}
