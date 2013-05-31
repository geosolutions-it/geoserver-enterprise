/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;


/**
 * Panel for the basic dbms parameters
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class BasicSDEParamPanel extends Panel
{
    String host;

    int port = 5151;

    String username;

    String password;

    String instance;

    int minConnections = 2;

    int maxConnections = 6;

    int connTimeout = 500;

    public BasicSDEParamPanel(String id)
    {
        super(id);

        add(new TextField("host", new PropertyModel(this, "host")).setRequired(true));
        add(new TextField("port", new PropertyModel(this, "port")).setRequired(true));
        add(new TextField("username", new PropertyModel(this, "username")).setRequired(true));
        add(new PasswordTextField("password", new PropertyModel(this, "password")).setResetPassword(false));
        add(new TextField("instance", new PropertyModel(this, "instance")));
        add(new TextField("minConnections", new PropertyModel(this, "minConnections")));
        add(new TextField("maxConnections", new PropertyModel(this, "maxConnections")));
        add(new TextField("connTimeout", new PropertyModel(this, "connTimeout")));
    }


}
