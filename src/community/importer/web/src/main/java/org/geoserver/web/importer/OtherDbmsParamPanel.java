/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;


import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;


/**
 * Other params form for databases: schema, loose bbox, pk metadata lookup table
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class OtherDbmsParamPanel extends Panel
{
    String schema;
    boolean userSchema;
    boolean excludeGeometryless = true;
    boolean looseBBox = true;
    String pkMetadata;
    WebMarkupContainer advancedContainer;
    private WebMarkupContainer advancedPanel;
    private WebMarkupContainer userSchemaContainer;
    private WebMarkupContainer userSchemaChkContainer;

    public OtherDbmsParamPanel(String id, String defaultSchema, boolean showUserSchema, boolean showLooseBBox)
    {
        super(id);
        this.schema = defaultSchema;

        // we create a global container in order to update the visibility of the various items
        // at runtime
        final WebMarkupContainer basicParams = new WebMarkupContainer("basicParams");
        basicParams.setOutputMarkupId(true);
        add(basicParams);

        // we allow defaulting to the user name schema for Oracle
        // ... the checkbox to default to the user schema
        userSchemaChkContainer = new WebMarkupContainer("userSchemaChkContainer");
        basicParams.add(userSchemaChkContainer);

        CheckBox userSchemaChk = new CheckBox("userSchema", new PropertyModel(this, "userSchema"));
        userSchemaChk.add(new AjaxFormComponentUpdatingBehavior("onClick")
            {

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    userSchemaContainer.setVisible(!userSchema);
                    target.addComponent(basicParams);
                }
            });
        userSchemaChkContainer.add(userSchemaChk);
        // the custom schema chooser
        userSchemaContainer = new WebMarkupContainer("userSchemaContainer");
        basicParams.add(userSchemaContainer);

        TextField schemaTxt = new TextField("schema", new PropertyModel(this, "schema"));
        userSchemaContainer.add(schemaTxt);
        setShowUserSchema(showUserSchema);

        basicParams.add(new CheckBox("excludeGeometryless", new PropertyModel(this, "excludeGeometryless")));
        add(toggleAdvanced());

        advancedContainer = new WebMarkupContainer("advancedContainer");
        advancedContainer.setOutputMarkupId(true);
        advancedPanel = new WebMarkupContainer("advanced");
        advancedPanel.setVisible(false);

        WebMarkupContainer looseBBoxContainer = new WebMarkupContainer("looseBBoxContainer");
        looseBBoxContainer.setVisible(showLooseBBox);

        CheckBox fastBBoxCheck = new CheckBox("looseBBox", new PropertyModel(this, "looseBBox"));
        looseBBoxContainer.add(fastBBoxCheck);
        advancedPanel.add(looseBBoxContainer);
        advancedPanel.add(new TextField("pkMetadata", new PropertyModel(this, "pkMetadata")));
        advancedContainer.add(advancedPanel);
        add(advancedContainer);
    }

    public void setShowUserSchema(boolean showUserSchema)
    {
        if (showUserSchema)
        {
            userSchema = true;
            userSchemaContainer.setVisible(false);
            userSchemaChkContainer.setVisible(true);
        }
        else
        {
            userSchema = false;
            userSchemaChkContainer.setVisible(false);
            userSchemaContainer.setVisible(true);
        }
    }

    Component toggleAdvanced()
    {
        final AjaxLink advanced = new AjaxLink("advancedLink")
            {

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    advancedPanel.setVisible(!advancedPanel.isVisible());
                    target.addComponent(advancedContainer);
                    target.addComponent(this);
                }
            };
        advanced.add(new AttributeModifier("class", true, new AbstractReadOnlyModel()
                {

                    @Override
                    public Object getObject()
                    {
                        return advancedPanel.isVisible() ? "expanded" : "collapsed";
                    }
                }));
        advanced.setOutputMarkupId(true);

        return advanced;
    }

}
