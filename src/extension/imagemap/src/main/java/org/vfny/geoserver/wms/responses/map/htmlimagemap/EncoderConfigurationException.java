/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import org.geoserver.platform.ServiceException;


/**
 * DOCUMENT ME!
 *
 * @author Mauro Bartolomeoli 
 */
public class EncoderConfigurationException extends ServiceException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -791532675720744977L;

	public EncoderConfigurationException(String message) {
        super(message);
    }

    public EncoderConfigurationException(String message, String locator) {
        super(message, locator);
    }

    public EncoderConfigurationException(Throwable e, String message, String locator) {
        super(e, message, locator);
    }
}
