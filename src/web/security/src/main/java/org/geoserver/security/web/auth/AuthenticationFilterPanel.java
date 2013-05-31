/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;

/**
 * Base class for authentication filter panels.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AuthenticationFilterPanel<T extends SecurityAuthFilterConfig> 
    extends SecurityNamedServicePanel<T> {

    public AuthenticationFilterPanel(String id, IModel<T> model) {
        super(id, model);

        //add(new ChainCategoryChoice("chains", new Model()));
    }

    @Override
    public void doSave(T config) throws Exception {
        getSecurityManager().saveFilter(config);
    }

    @Override
    public void doLoad(T config) throws Exception {
        getSecurityManager().loadFilter(config.getName());
    }
}
