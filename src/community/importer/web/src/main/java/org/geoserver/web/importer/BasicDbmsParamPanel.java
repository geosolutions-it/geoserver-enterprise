/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;


/**
 * Panel for the basic dbms parameters
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class BasicDbmsParamPanel extends Panel
{
    String host;

    int port;

    String username;

    String password;

    String database;

    ConnectionPoolParamPanel connPool;

    WebMarkupContainer connPoolParametersContainer;

    Component connPoolLink;

    public BasicDbmsParamPanel(String id, String host, int port, boolean databaseRequired)
    {
        this(id, host, port, null, null, databaseRequired);
    }

    public BasicDbmsParamPanel(String id, String host, int port, String database, String username,
        boolean databaseRequired)
    {
        super(id);
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;

        add(new TextField("host", new PropertyModel(this, "host")).setRequired(true));
        add(new TextField("port", new PropertyModel(this, "port")).setRequired(true));
        add(new TextField("username", new PropertyModel(this, "username")).setRequired(true));
        add(new PasswordTextField("password", new PropertyModel(this, "password")).setResetPassword(false).setRequired(false));
        add(new TextField("database", new PropertyModel(this, "database")).setRequired(databaseRequired));

        connPoolLink = toggleConnectionPoolLink();
        add(connPoolLink);
        connPoolParametersContainer = new WebMarkupContainer("connPoolParametersContainer");
        connPoolParametersContainer.setOutputMarkupId(true);
        connPool = new ConnectionPoolParamPanel("connPoolParameters", true);
        connPool.setVisible(false);
        connPoolParametersContainer.add(connPool);
        add(connPoolParametersContainer);
    }

    /**
     * Toggles the connection pool param panel
     *
     * @return
     */
    Component toggleConnectionPoolLink()
    {
        AjaxLink connPoolLink = new AjaxLink("connectionPoolLink")
            {

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    connPool.setVisible(!connPool.isVisible());
                    target.addComponent(connPoolParametersContainer);
                    target.addComponent(this);
                }
            };
        connPoolLink.add(new AttributeModifier("class", true, new AbstractReadOnlyModel()
                {

                    @Override
                    public Object getObject()
                    {
                        return connPool.isVisible() ? "expanded" : "collapsed";
                    }
                }));
        connPoolLink.setOutputMarkupId(true);

        return connPoolLink;
    }

}
