/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.web.wicket.XMLNameValidator;

/**
 * Allows creation of a new workspace
 */
@SuppressWarnings("serial")
public class WorkspaceNewPage extends GeoServerSecuredPage {

    Form form;
    TextField nsUriTextField;
    boolean defaultWs;
    
    public WorkspaceNewPage() {
        WorkspaceInfo ws = getCatalog().getFactory().createWorkspace();
        
        form = new Form( "form", new CompoundPropertyModel( ws ) ) {
            @Override
            protected void onSubmit() {
                Catalog catalog = getCatalog();
                
                WorkspaceInfo ws = (WorkspaceInfo) form.getModelObject();
                
                NamespaceInfo ns = catalog.getFactory().createNamespace();
                ns.setPrefix ( ws.getName() );
                ns.setURI(nsUriTextField.getDefaultModelObjectAsString());
                
                catalog.add( ws );
                catalog.add( ns );
                if(defaultWs)
                    catalog.setDefaultWorkspace(ws);
                
                //TODO: set the response page to be the edit 
                doReturn(WorkspacePage.class);
            }
        };
        add(form);
        
        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setRequired(true);
        nameTextField.add(new XMLNameValidator());
        nameTextField.add(new StringValidator() {
            
            @Override
            protected void onValidate(IValidatable<String> validatable) {
                if(CatalogImpl.DEFAULT.equals(validatable.getValue())) {
                    error(validatable, "defaultWsError");
                }
            }
        });
        form.add( nameTextField.setRequired(true) );
        
        nsUriTextField = new TextField( "uri", new Model() );
        // maybe a bit too restrictive, but better than not validation at all
        nsUriTextField.setRequired(true);
        nsUriTextField.add(new URIValidator());
        form.add( nsUriTextField );
        
        CheckBox defaultChk = new CheckBox("default", new PropertyModel(this, "defaultWs"));
        form.add(defaultChk);
        
        SubmitLink submitLink = new SubmitLink( "submit", form );
        form.add( submitLink );
        form.setDefaultButton(submitLink);
        
        AjaxLink cancelLink = new AjaxLink( "cancel" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn(WorkspacePage.class);
            }
        };
        form.add( cancelLink );
        
    }
}
