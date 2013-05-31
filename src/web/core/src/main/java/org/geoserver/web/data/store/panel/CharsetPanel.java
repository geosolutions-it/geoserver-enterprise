/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A label + locale dropdown form panel
 */
@SuppressWarnings("serial")
public class CharsetPanel extends Panel implements ParamPanel {

    private DropDownChoice choice;
    
    public CharsetPanel(final String id, final IModel charsetModel,
            final IModel paramLabelModel, final boolean required) {
        // make the value of the combo field the model of this panel, for easy
        // value retriaval
        super(id, charsetModel);

        // the label
        String requiredMark = required ? " *" : ""; 
        Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
        add(label);

        // the drop down field, with a decorator for validations
        final ArrayList charsets = new ArrayList(Charset.availableCharsets().keySet());
        choice = new DropDownChoice("paramValue", charsetModel, charsets);
        choice.setRequired(required);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        choice.setLabel(paramLabelModel);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("border");
        feedback.add(choice);
        add(feedback);
    }

    /**
     * Returns the form component used in the panel in case it is needed for related form components
     * validation
     * 
     * @return
     */
    public FormComponent getFormComponent() {
        return choice;
    }
    
}
