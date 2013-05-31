/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.FeatureId;

/**
 * Extracts feature type names from any Id filters. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class TypeNameExtractingVisitor extends DefaultFilterVisitor {

    Catalog catalog;
    Set<QName> typeNames = new TreeSet<QName>();

    public TypeNameExtractingVisitor(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Object visit(Id filter, Object data) {
        for (Identifier id : filter.getIdentifiers()) {
            if (id instanceof FeatureId) {
                FeatureId fid = (FeatureId) id;
                if (fid.getID() != null) {
                    String[] split = fid.getID().split("\\.");
                    if (split.length > 1) {
                        ResourceInfo r = catalog.getResourceByName(split[0], ResourceInfo.class);
                        if (r != null) {
                            typeNames.add(new QName(r.getNamespace().getURI(), r.getName()));
                        }
                    }
                }
            }
        }
        return data;
    }

    public Set<QName> getTypeNames() {
        return typeNames;
    }
}
