/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;


import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;


/**
 * Other params form for ArcSDE: version and eclude geometryless
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
class OtherSDEParamPanel extends Panel
{
    String version;
    boolean excludeGeometryless = true;

    public OtherSDEParamPanel(String id)
    {
        super(id);

        // the version chooser
        add(new TextField("version", new PropertyModel(this, "version")));
        add(new CheckBox("excludeGeometryless", new PropertyModel(this, "excludeGeometryless")));
    }

}
