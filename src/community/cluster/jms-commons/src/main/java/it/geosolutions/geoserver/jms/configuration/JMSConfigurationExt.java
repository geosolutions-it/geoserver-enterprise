/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

import java.io.IOException;

public interface JMSConfigurationExt {

	/**
	 * initialize defaults
	 * @param config
	 * @throws IOException
	 */
	public void initDefaults(JMSConfiguration config) throws IOException;

	/**
	 * returns true if the passed config is different by the default values
	 * @param config
	 * @return
	 * @throws IOException
	 */
	public boolean override(JMSConfiguration config) throws IOException;

}
