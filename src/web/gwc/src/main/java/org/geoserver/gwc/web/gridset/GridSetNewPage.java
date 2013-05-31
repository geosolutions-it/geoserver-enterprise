/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.gwc.GWC;
import org.geowebcache.grid.GridSet;

public class GridSetNewPage extends AbstractGridSetPage {

    public GridSetNewPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onSave(AjaxRequestTarget target, Form<?> form) {
        GridSetInfo info = (GridSetInfo) form.getModelObject();

        GridSet gridset;
        try {
            gridset = GridSetBuilder.build(info);
        } catch (IllegalStateException e) {
            form.error(e.getMessage());
            target.addComponent(form);
            return;
        }

        try {
            GWC gwc = GWC.get();
            gwc.addGridSet(gridset);
            doReturn(GridSetsPage.class);
        } catch (Exception e) {
            form.error(e.getMessage());
            target.addComponent(form);
        }
    }

}
