/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geotools.xml.Schemas;

/**
 * Cleans up the temporarily created schemas when the request ends
 *  
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SchemaCleanerCallback extends AbstractDispatcherCallback {

    static final ThreadLocal<List<XSDSchema>> schemas = new ThreadLocal<List<XSDSchema>>();
    
    /**
     * Schedules a XSDSchema for removal at the end of the request
     * @param schema
     */
    public static void addSchema(XSDSchema schema) {
        if(schema == null) {
            return;
        }
        
        List<XSDSchema> list = schemas.get();
        if(list == null) {
            list = new ArrayList<XSDSchema>();
            schemas.set(list);
        }
        list.add(schema);
    }
    
    @Override
    public void finished(Request request) {
        List<XSDSchema> list = schemas.get();
        if(list != null) {
            schemas.remove();
            for (XSDSchema schema : list) {
                Schemas.dispose(schema);
            }
        }
    }
}
