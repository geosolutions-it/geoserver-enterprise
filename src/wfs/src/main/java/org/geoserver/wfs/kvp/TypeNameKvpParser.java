/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.opengis.feature.type.Name;

/**
 * Parses a {@code typeName} GetFeature parameter the form "([prefix:]local)+".
 * <p>
 * This parser will parse strings of the above format into a list of
 * {@link javax.xml.namespace.QName}
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @author groldan
 */
public class TypeNameKvpParser extends QNameKvpParser {

    GeoServer geoserver;

    public TypeNameKvpParser(String key, GeoServer geoserver, Catalog catalog) {
        super(key, catalog, false);
        this.geoserver = geoserver;
    }
    
    protected Object parseToken(String token) throws Exception {
        int i = token.indexOf(':');

        if (i != -1 || geoserver.getService(WFSInfo.class).isCiteCompliant()) {
            return super.parseToken(token);
        } else {
            // we don't have the namespace, use the catalog to lookup the feature type 
            // mind, this is lenient behavior so we use it only if the server is not runnig in cite mode
            FeatureTypeInfo ftInfo = catalog.getFeatureTypeByName(token);
            if(ftInfo == null) {
                throw new WFSException("Could not find type name " + token, "InvalidParameterValue", "typeName");
            } else {
                final Name name = ftInfo.getFeatureType().getName();
                return new QName(name.getNamespaceURI(), name.getLocalPart());
            }
        }
    }
    
}
