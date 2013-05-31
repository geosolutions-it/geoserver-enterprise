/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Version;

/**
 * Style edit page
 */
public class StyleEditPage extends AbstractStylePage {
    
    public static final String NAME = "name";
    public static final String WORKSPACE = "workspace";

    public StyleEditPage(PageParameters parameters) {
        String name = parameters.getString(NAME);
        String workspace = parameters.getString(WORKSPACE);

        StyleInfo si = workspace != null ? getCatalog().getStyleByName(workspace, name) : 
            getCatalog().getStyleByName(name);
        
        if(si == null) {
            error(new ParamResourceModel("StyleEditPage.notFound", this, name).getString());
            doReturn(StylePage.class);
            return;
        }
        
        initUI(si);

        if (!isAuthenticatedAsAdmin()) {
            Form f = (Form)get("form");
    
            //global styles only editable by full admin
            if (si.getWorkspace() == null) {
                styleForm.setEnabled(false);
                nameTextField.setEnabled(false);
                uploadForm.setEnabled(false);

                editor.add(new AttributeAppender("class", new Model("disabled"), " "));
                get("validate").add(new AttributeAppender("style", new Model("display:none;"), " "));
                add(new AbstractBehavior() {
                    @Override
                    public void renderHead(IHeaderResponse response) {
                        response.renderOnLoadJavascript(
                            "document.getElementById('mainFormSubmit').style.display = 'none';");
                        response.renderOnLoadJavascript(
                            "document.getElementById('uploadFormSubmit').style.display = 'none';");
                    }
                });

                info(new StringResourceModel("globalStyleReadOnly", this, null).getString());
            }

            //always disable the workspace toggle
            f.get("workspace").setEnabled(false);
        }
    }
    
    public StyleEditPage(StyleInfo style) {
        super(style);
        uploadForm.setVisible(false);
    }

    @Override
    protected void onStyleFormSubmit() {
        // write out the file and save name modifications
        try {
            StyleInfo style = (StyleInfo) styleForm.getModelObject();
            Version version = Styles.findVersion(new ByteArrayInputStream(rawSLD.getBytes()));
            style.setSLDVersion(version);
            
            // write out the SLD
            try {
                getCatalog().getResourcePool().writeStyle(style,
                        new ByteArrayInputStream(rawSLD.getBytes()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            getCatalog().save(style);
            doReturn( StylePage.class );
        } catch( Exception e ) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            styleForm.error( e );
        }
        
    }
    
}
