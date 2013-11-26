/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;

import javax.xml.namespace.QName;

import com.thoughtworks.xstream.XStream;
 
/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
public class FeatureAttributePPIO extends XStreamPPIO {
 
	static final QName FeatureAttributeResults = new QName("FeatureAttributeResults");
	
    protected FeatureAttributePPIO() {
        super(FeatureAttribute.class, FeatureAttributeResults);
    }

	@Override
	public FeatureAttributePPIO decode(InputStream input) throws Exception {
		// prepare xml encoding
        XStream xstream = buildXStream();

		// write out FeatureAttributePPIO
        return (FeatureAttributePPIO) xstream.fromXML(input);
	}
}