/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import org.geotools.wfs.WFSConfiguration;
import org.geotools.xml.Configuration;

/**
 * XML configuration for a geoserver application schema.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ApplicationSchemaConfiguration extends Configuration {

    public ApplicationSchemaConfiguration( ApplicationSchemaXSD xsd, WFSConfiguration config ){
        super( xsd );
        
        addDependency(config);
    }
}
