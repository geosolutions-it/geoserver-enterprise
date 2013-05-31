/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geotools.util.Version;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

/**
 * Xml reader for wfs 1.0.0 xml requests.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 * TODO: there is too much duplication with the 1.1.0 reader, factor it out.
 */
public class WfsXmlReader extends XmlRequestReader {
    /**
     * Xml Configuration
     */
    Configuration configuration;
    /**
     * geoserver configuration
     */
    GeoServer geoServer;

    public WfsXmlReader(String element, Configuration configuration, GeoServer geoServer) {
        this(element, configuration, geoServer, "wfs");
    }
    
    protected WfsXmlReader(String element, Configuration configuration, GeoServer geoServer, String serviceId) {
        super(new QName(WFS.NAMESPACE, element), new Version("1.0.0"), serviceId);
        this.configuration = configuration;
        this.geoServer = geoServer;
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        //TODO: refactor this method to use WFSXmlUtils
        Catalog catalog = geoServer.getCatalog();

        //check the strict flag to determine if we should validate or not
        Boolean strict = (Boolean) kvp.get("strict");
        if ( strict == null ) {
            strict = Boolean.FALSE;
        }
        
        //create the parser instance
        Parser parser = new Parser(configuration);
        
        //"inject" namespace mappings
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        for ( NamespaceInfo ns : namespaces ) {
            //if ( namespaces[i].isDefault() ) 
            //    continue;
            
            parser.getNamespaces().declarePrefix( 
                ns.getPrefix(), ns.getURI());
        }
        //set validation based on strict or not
        parser.setValidating(strict.booleanValue());
        parser.getURIHandlers().add(0, new WFSURIHandler(geoServer));

        //parse
        Object parsed = parser.parse(reader); 
        
        //if strict was set, check for validation errors and throw an exception 
        if (strict.booleanValue() && !parser.getValidationErrors().isEmpty()) {
            WFSException exception = new WFSException("Invalid request", "InvalidParameterValue");

            for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext();) {
                Exception error = (Exception) e.next();
                exception.getExceptionText().add(error.getLocalizedMessage());
            }

            throw exception;
        }
        
        return parsed;
    }
}
