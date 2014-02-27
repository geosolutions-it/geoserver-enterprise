/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.utils;

import java.io.IOException;
import java.util.Properties;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPropertyConfigurer;

public class JMSPropertyPlaceholderConfigurer extends GeoServerPropertyConfigurer {

	public JMSPropertyPlaceholderConfigurer(GeoServerDataDirectory data) {
		super(data);
	}
	
	public Properties[] getProperties(){
		return localProperties;
	}
	
	public Properties getMergedProperties() throws IOException{
		return mergeProperties();
	}

	
}
