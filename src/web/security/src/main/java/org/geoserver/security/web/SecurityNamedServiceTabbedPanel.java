/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Interface to be implemented by {@link SecurityNamedServicePanel} subclasses that should be 
 * layed out in a tabbed view.
 * <p>
 * This class must provide the additional tabs to display.
 * </p>
 *  
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface SecurityNamedServiceTabbedPanel<T extends SecurityNamedServiceConfig> {

    List<ITab> createTabs(IModel<T> model);

}
