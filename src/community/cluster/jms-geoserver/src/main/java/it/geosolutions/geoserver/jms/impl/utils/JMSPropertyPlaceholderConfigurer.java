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

import org.geoserver.data.util.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class JMSPropertyPlaceholderConfigurer extends
		PropertyPlaceholderConfigurer implements InitializingBean {

	private final JMSConfiguration config;
	private final Resource defaults;

	public JMSPropertyPlaceholderConfigurer(Resource defaultFile,
			JMSConfiguration config) throws IOException {
		if (!defaultFile.exists()) {
			throw new IOException(
					"Unable to locate the default properties file at:"
							+ defaultFile);
		}
		this.defaults = defaultFile;
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
		File properties = new File(config.getConfiguration(EmbeddedBrokerConfiguration.EMBEDDED_BROKER_PROPERTIES_KEY).toString());
		if (!properties.isAbsolute() && !properties.isFile()) {
			// try to resolve as absolute
			properties = new File(JMSConfiguration.getConfigPathDir(),
					properties.getPath());
			if (!properties.isFile()) {
				// copy the defaults
				IOUtils.copy(defaults.getInputStream(), properties);
			}
		}
		final Resource res = new FileSystemResource(properties);
		super.setLocation(res);
	}
}
