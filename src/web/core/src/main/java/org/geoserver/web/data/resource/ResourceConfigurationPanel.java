/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.logging.Logger;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.util.logging.Logging;

/**
 * A panel created to configure one aspect of a {@link ResourceInfo} object.
 * <p>
 * Typically there will be one panel dealing generically with
 * {@link ResourceInfo} and one extra panel to deal with the specifics of each
 * subclass.
 * <p>
 * All the components in the panel must be contained in a {@link Form} to make
 * sure the whole tab switch and page submit workflow function properly
 */
@SuppressWarnings("serial")
public class ResourceConfigurationPanel extends Panel {
    protected static Logger LOGGER = Logging.getLogger(ResourceConfigurationPanel.class);

	public ResourceConfigurationPanel(String panelId, IModel model){
		super(panelId, model);
	}
	
	public ResourceInfo getResourceInfo(){
		return (ResourceInfo)getDefaultModelObject();
	}
}
