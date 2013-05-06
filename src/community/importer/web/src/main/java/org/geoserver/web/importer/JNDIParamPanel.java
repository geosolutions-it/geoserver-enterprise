/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;


import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;


/**
 * JNDI params form
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class JNDIParamPanel extends Panel
{
    String jndiReferenceName;

    public JNDIParamPanel(String id, String jndiReferenceName)
    {
        super(id);
        this.jndiReferenceName = jndiReferenceName;

        add(new TextField("jndiReferenceName", new PropertyModel(this, "jndiReferenceName")).setRequired(true));
    }

}
