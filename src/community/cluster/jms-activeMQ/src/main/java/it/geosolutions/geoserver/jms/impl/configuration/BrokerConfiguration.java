/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;
import it.geosolutions.geoserver.jms.impl.utils.JMSPropertyPlaceholderConfigurer;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 
 * Abstract class to store and load configuration from global var or
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class BrokerConfiguration implements JMSConfigurationExt {

	public static final String BROKER_URL_KEY = "brokerURL";
	public static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

	@Autowired
	@Qualifier("JMSPropertyConfigurer")
	JMSPropertyPlaceholderConfigurer commonConfiguration;

	@Override
	public void initDefaults(JMSConfiguration config) throws IOException {
		String url = null;

		if (commonConfiguration != null) {
			url = commonConfiguration.getMergedProperties().getProperty(
					BROKER_URL_KEY);
		}

		config.putConfiguration(BROKER_URL_KEY, url != null ? url
				: DEFAULT_BROKER_URL);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) throws IOException {
		boolean override = config.checkForOverride(BROKER_URL_KEY,
				DEFAULT_BROKER_URL);
		if (!override) {
			initDefaults(config);
			override = true;
		}
		return override;
	}

}
