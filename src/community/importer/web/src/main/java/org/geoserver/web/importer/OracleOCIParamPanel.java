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
 * Panel for the Oracle OCI params panel
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class OracleOCIParamPanel extends Panel
{
    String alias;

    String username;

    String password;

    public OracleOCIParamPanel(String id)
    {
        super(id);

        add(new TextField("alias", new PropertyModel(this, "alias")).setRequired(true));
        add(new TextField("username", new PropertyModel(this, "username")).setRequired(true));
        add(new PasswordTextField("password", new PropertyModel(this, "password")).setResetPassword(false));
    }

}
