package org.geoserver.security.web.auth;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerBasicAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class BasicAuthFilterPanel extends AuthenticationFilterPanel<BasicAuthenticationFilterConfig>{

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public BasicAuthFilterPanel(String id, IModel<BasicAuthenticationFilterConfig> model) {
        super(id, model);

        add(new CheckBox("useRememberMe"));
    }

}
