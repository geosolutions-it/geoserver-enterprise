/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.ParamResourceModel;

import com.vividsolutions.jts.geom.Geometry;

@SuppressWarnings("serial")
public class AttributeEditPage extends GeoServerSecuredPage {

    AttributeDescription attribute;

    NewFeatureTypePage previousPage;

    WebMarkupContainer crsContainer;

    WebMarkupContainer sizeContainer;

    boolean newAttribute;

    private TextField nameField;

    String size;

    private TextField sizeField;

    private CRSPanel crsField;

    public AttributeEditPage(final AttributeDescription attribute,
            final NewFeatureTypePage previousPage) {
        this(attribute, previousPage, false);
    }

    AttributeEditPage(final AttributeDescription attribute, final NewFeatureTypePage previousPage,
            final boolean newAttribute) {
        this.previousPage = previousPage;
        this.newAttribute = newAttribute;
        this.attribute = attribute;
        this.size = String.valueOf(attribute.getSize());

        final Form form = new Form("form", new CompoundPropertyModel(attribute));
        form.setOutputMarkupId(true);
        add(form);

        form.add(nameField = new TextField("name"));
        DropDownChoice binding = new DropDownChoice("binding", AttributeDescription.BINDINGS,
                new BindingChoiceRenderer());
        binding.add(new AjaxFormSubmitBehavior("onchange") {

            @Override
            protected void onError(AjaxRequestTarget target) {
                updateVisibility(target);
            }

            private void updateVisibility(AjaxRequestTarget target) {
                sizeContainer.setVisible(String.class.equals(attribute.getBinding()));
                crsContainer.setVisible(attribute.getBinding() != null
                        && Geometry.class.isAssignableFrom(attribute.getBinding()));

                target.addComponent(getFeedbackPanel());
                target.addComponent(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                updateVisibility(target);
            }
        });
        form.add(binding);
        form.add(new CheckBox("nullable"));

        sizeContainer = new WebMarkupContainer("sizeContainer");
        sizeContainer.setOutputMarkupId(true);
        form.add(sizeContainer);
        sizeContainer.add(sizeField = new TextField("size", new PropertyModel(this, "size")));
        sizeContainer.setVisible(String.class.equals(attribute.getBinding()));

        crsContainer = new WebMarkupContainer("crsContainer");
        crsContainer.setOutputMarkupId(true);
        form.add(crsContainer);
        crsContainer.add(crsField = new CRSPanel("crs"));
        crsContainer.setVisible(attribute.getBinding() != null
                && Geometry.class.isAssignableFrom(attribute.getBinding()));

        SubmitLink submit = new SubmitLink("save") {
            @Override
            public void onSubmit() {
                if (validate()) {
                    if (newAttribute) {
                        previousPage.attributesProvider.addNewAttribute(attribute);
                    }
                    setResponsePage(previousPage);
                }
            }

        };
        form.setDefaultButton(submit);
        form.add(submit);
        form.add(new Link("cancel") {

            @Override
            public void onClick() {
                setResponsePage(previousPage);
            }
        });

    }

    /**
     * We have to resort to manual validation otherwise the ajax tricks performed by the drop down
     * won't work
     */
    protected boolean validate() {
        boolean valid = true;
        if (attribute.getName() == null || attribute.getName().trim().equals("")) {
            nameField.error((IValidationError) new ValidationError().addMessageKey("Required"));
            valid = false;
        }
        if (String.class.equals(attribute.getBinding())) {
            try {
                attribute.setSize(Integer.parseInt(size));
                if (attribute.getSize() <= 0) {
                    sizeField.error(new ParamResourceModel("notPositive", this));
                    valid = false;
                }
            } catch (Exception e) {
                sizeField.error(new ParamResourceModel("notInteger", this, size));
                valid = false;
            }

        }
        if (Geometry.class.isAssignableFrom(attribute.getBinding()) && attribute.getCrs() == null) {
            crsField.error((IValidationError) new ValidationError().addMessageKey("Required"));
            valid = false;
        }

        return valid;
    }

    static class BindingChoiceRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return AttributeDescription.getLocalizedName((Class) object);
        }

        public String getIdValue(Object object, int index) {
            return ((Class) object).getName();
        }

    }
}
