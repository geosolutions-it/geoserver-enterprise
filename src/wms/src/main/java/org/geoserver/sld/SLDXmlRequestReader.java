/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.Reader;
import java.util.Map;

import org.geoserver.ows.XmlRequestReader;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;

/**
 * Reads 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class SLDXmlRequestReader extends XmlRequestReader {

    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    
    private WMS wms;
    
    public SLDXmlRequestReader(WMS wms) {
        super("http://www.opengis.net/sld", "StyledLayerDescriptor" );
        this.wms = wms;
    }

    public void setStyleFactory(StyleFactory styleFactory) {
        this.styleFactory = styleFactory;
    }
    
   
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        if ( request == null ) {
            throw new IllegalArgumentException( "request must be not null" );
        }
        
        GetMapRequest getMap = (GetMapRequest) request;
        StyledLayerDescriptor sld = 
            new SLDParser( styleFactory, reader ).parseSLD();
        
        //process the sld 
        GetMapKvpRequestReader.processStandaloneSld(wms, getMap, sld);
    
        return getMap;
    }
    
}
