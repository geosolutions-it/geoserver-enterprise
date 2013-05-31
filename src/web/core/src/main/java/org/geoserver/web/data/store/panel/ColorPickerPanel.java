/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import java.awt.Color;
import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.ColorPickerField;

/**
 * A label with a text field. Can receive custom validators for the text field.
 * 
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class ColorPickerPanel extends Panel {

	/**
	 * 
	 * @param id
	 * @param paramsMap
	 * @param paramName
	 * @param paramLabel
	 * @param required
	 * @param validators
	 *            any extra validator that should be added to the input field,
	 *            or {@code null}
	 */
	public ColorPickerPanel(final String id, final IModel paramVale,
			final IModel paramLabelModel, final boolean required,
			IValidator... validators) {
		// make the value of the text field the model of this panel, for easy
		// value retriaval
		super(id, paramVale);

		// the label
		String requiredMark = required ? " *" : ""; 
		Label label = new Label("paramName", paramLabelModel.getObject() + requiredMark);
		add(label);

		// the color picker. Notice that we need to convert between RRGGBB and
		// #RRGGBB,
		// passing in a Color.class param is just a trick to force the component
		// to use
		// the converter both ways
		ColorPickerField textField = new ColorPickerField("paramValue",
				paramVale, Color.class) {
			@Override
			public IConverter getConverter(Class type) {
				return new IConverter() {

					public String convertToString(Object value, Locale locale) {
						String input = (String) value;
						if (input.startsWith("#"))
							return input.substring(1);
						else
							return input;
					}

					public Object convertToObject(String value, Locale locale) {
						if (value.equals(""))
							return value;
						return "#" + value;
					}
				};
			}
		};
		textField.setRequired(required);
		// set the label to be the paramLabelModel otherwise a validation error
		// would look like
		// "Parameter 'paramValue' is required"
		textField.setLabel(paramLabelModel);

		if (validators != null) {
			for (IValidator validator : validators) {
				textField.add(validator);
			}
		}
		FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder(
				"border");
		feedback.add(textField);
		add(feedback);
	}
}
