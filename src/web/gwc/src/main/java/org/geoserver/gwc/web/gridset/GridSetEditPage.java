/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.gwc.GWC;
import org.geowebcache.grid.GridSet;

public class GridSetEditPage extends AbstractGridSetPage {

    private String originalName;

    public GridSetEditPage(PageParameters parameters) {
        super(parameters);

        GridSetInfo info = form.getModelObject();
        originalName = info.getName();

        if (info.isInternal()) {
            form.info(new ResourceModel("GridSetEditPage.internalGridSetMessage").getObject());
            name.getFormComponent().setEnabled(false);
            description.setEnabled(false);
            crs.setEnabled(false);
            tileWidth.getFormComponent().setEnabled(false);
            tileHeight.getFormComponent().setEnabled(false);
            bounds.setEnabled(false);
            computeBoundsLink.setEnabled(false);
            tileMatrixSetEditor.setEnabled(false);
            saveLink.setVisible(false);
            addLevelLink.setVisible(false);
        }
    }

    @Override
    protected void onSave(AjaxRequestTarget target, Form<?> form) {
        GridSetInfo info = (GridSetInfo) form.getModelObject();

        GWC gwc = GWC.get();

        final GridSet newGridset;
        try {
            newGridset = GridSetBuilder.build(info);
        } catch (IllegalStateException e) {
            form.error(e.getMessage());
            target.addComponent(form);
            return;
        }

        try {
            // TODO: warn and eliminate caches
            gwc.modifyGridSet(originalName, newGridset);
            doReturn(GridSetsPage.class);
        } catch (Exception e) {
            e.printStackTrace();
            form.error("Error saving gridset: " + e.getMessage());
            target.addComponent(form);
        }
    }

}
