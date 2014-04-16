/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.utils;

import it.geosolutions.geoserver.jms.configuration.EmbeddedBrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class JMSPropertyPlaceholderConfigurer extends
		PropertyPlaceholderConfigurer implements InitializingBean {

	private final JMSConfiguration config;
	private final File defaults;

	public JMSPropertyPlaceholderConfigurer(Resource defaultFile,
			JMSConfiguration config) throws IOException {
		defaults = defaultFile.getFile();
		if (!defaults.isFile()) {
			throw new IOException(
					"Unable to locate the default properties file at:"
							+ defaultFile);
		}
		this.config = config;
	}

	public Properties[] getProperties() {
		return localProperties;
	}

	public Properties getMergedProperties() throws IOException {
		return mergeProperties();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		File properties = new File(config.getConfiguration(
				EmbeddedBrokerConfiguration.EMBEDDED_BROKER_PROPERTIES_KEY)
				.toString());
		if (!properties.isAbsolute() && !properties.isFile()) {
			// try to resolve as absolute
			properties = new File(JMSConfiguration.getConfigPathDir(),
					properties.getPath());
			if (!properties.isFile()) {
				// copy the defaults
				FileUtils.copyFile(defaults, properties);
			}
		}
		final Resource res = new FileSystemResource(properties);
		super.setLocation(res);
	}
}
