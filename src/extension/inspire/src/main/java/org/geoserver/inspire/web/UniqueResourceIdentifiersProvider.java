/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.wicket.GeoServerDataProvider;

@SuppressWarnings("serial")
public class UniqueResourceIdentifiersProvider extends
        GeoServerDataProvider<UniqueResourceIdentifier> {

    IModel<UniqueResourceIdentifiers> model;

    public UniqueResourceIdentifiersProvider(IModel<UniqueResourceIdentifiers> model) {
        this.model = model;
    }

    @Override
    protected List<Property<UniqueResourceIdentifier>> getProperties() {
        return Arrays.asList(
                new BeanProperty<UniqueResourceIdentifier>("code", "code"),
                new BeanProperty<UniqueResourceIdentifier>("namespace", "namespace"),
                // for the moment we leave this one out, while it's in the XSD no example
                // ever shows how to use it, it's most likely used somewhere else in INSPIRE, not in DLS
                // new BeanProperty<UniqueResourceIdentifier>("metadataURL", "metadataURL"),
                new PropertyPlaceholder<UniqueResourceIdentifier>("remove")
                );
    }

    @Override
    protected List<UniqueResourceIdentifier> getItems() {
        return model.getObject();
    }

}
