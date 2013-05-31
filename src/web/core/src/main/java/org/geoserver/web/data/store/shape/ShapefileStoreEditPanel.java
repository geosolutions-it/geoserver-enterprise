/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.shape;

import static org.geotools.data.shapefile.ShapefileDataStoreFactory.*;

import java.util.logging.Logger;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CharsetPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geotools.util.logging.Logging;

/**
 * Provides the form components for the shapefile datastore
 * 
 * @author Andrea Aime - GeoSolution
 * 
 */
@SuppressWarnings("serial")
public class ShapefileStoreEditPanel extends StoreEditPanel {

    private static final Logger LOGGER = Logging.getLogger(ShapefileStoreEditPanel.class);

    public ShapefileStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        FileParamPanel file = buildFileParamPanel(paramsModel);
        add(file);

        add(new CharsetPanel("charset", new MapModel(paramsModel, DBFCHARSET.key),
                new ParamResourceModel("charset", this), false));

        add(new CheckBoxParamPanel("memoryMapped", new MapModel(paramsModel, MEMORY_MAPPED.key),
                new ParamResourceModel("memoryMapped", this)));
        add(new CheckBoxParamPanel("cacheMemoryMaps", new MapModel(paramsModel,
                CACHE_MEMORY_MAPS.key), new ParamResourceModel("cacheMemoryMaps", this)));

        add(new CheckBoxParamPanel("spatialIndex", new MapModel(paramsModel,
                CREATE_SPATIAL_INDEX.key), new ParamResourceModel("spatialIndex", this)));
    }

    protected FileParamPanel buildFileParamPanel(final IModel paramsModel) {
        FileParamPanel file = new FileParamPanel("url", new MapModel(paramsModel, URLP.key),
                new ParamResourceModel("shapefile", this), true);
        file.setFileFilter(new Model(new ExtensionFileFilter(".shp")));
        file.getFormComponent().add(new FileExistsValidator());
        return file;
    }

}
