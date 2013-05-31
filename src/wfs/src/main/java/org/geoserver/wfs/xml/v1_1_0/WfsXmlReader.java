/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.io.Reader;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.WFSURIHandler;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geotools.util.Version;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;

/**
 * Xml reader for wfs 1.1.0 xml requests.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 * TODO: there is too much duplication with the 1.0.0 reader, factor it out.
 */
public class WfsXmlReader extends XmlRequestReader {
    /**
     * WFs configuration
     */
    WFSInfo wfs;

    /**
     * Xml Configuration
     */
    Configuration configuration;
    
    /**
     * geoserver configuartion
     */
    GeoServer geoServer;

    public WfsXmlReader(String element, GeoServer gs, Configuration configuration) {
        this(element, gs, configuration, "wfs");
    }
    
    protected WfsXmlReader(String element, GeoServer gs, Configuration configuration, String serviceId) {
        super(new QName(org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE, element), new Version("1.1.0"),
            serviceId);
        this.geoServer = gs;
        this.wfs = gs.getService( WFSInfo.class );
        this.configuration = configuration;
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        //TODO: make this configurable?
        configuration.getProperties().add(Parser.Properties.PARSE_UNKNOWN_ELEMENTS);

        Parser parser = new Parser(configuration);
        WFSXmlUtils.initRequestParser(parser, wfs, geoServer, kvp);
        
        Object parsed = WFSXmlUtils.parseRequest(parser, reader, wfs);
        parser.getURIHandlers().add(0, new WFSURIHandler(geoServer));
        
        WFSXmlUtils.checkValidationErrors(parser, this);
        
        return parsed;
    }
}
