/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import java.util.logging.Logger;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geotools.util.logging.Logging;

/**
 * Abstract edit component for file based rasters
 * 
 * @author Andrea Aime - GeoSolution
 * 
 */
@SuppressWarnings("serial")
public abstract class AbstractRasterFileEditPanel extends StoreEditPanel {

    private static final Logger LOGGER = Logging.getLogger(AbstractRasterFileEditPanel.class);

    public AbstractRasterFileEditPanel(final String componentId, final Form storeEditForm,
            String... fileExtensions) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        FileParamPanel file = new FileParamPanel("url", new PropertyModel(model, "URL"),
                new ResourceModel("url", "URL"), true);
        file.getFormComponent().add(new FileExistsValidator());
        if (fileExtensions != null && fileExtensions.length > 0) {
            file.setFileFilter(new Model(new ExtensionFileFilter(fileExtensions)));
        }
        add(file);
    }

}
