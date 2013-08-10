/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;
import it.geosolutions.geoserver.jms.impl.utils.JMSPropertyPlaceholderConfigurer;

import java.io.File;
import java.io.IOException;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * 
 * Abstract class to store and load configuration from global var or
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
@DependsOn("GeoServerDataDirectory")
final public class BrokerConfiguration implements JMSConfigurationExt {

	public static final String BROKER_URL_KEY = "brokerURL";
	public static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

	public BrokerConfiguration() throws IllegalArgumentException, IOException {
		// override default storage dir

		// final File root = GeoServerExtensions
		// .bean(GeoServerDataDirectory.class).findDataRoot();
		// if (root != null) {
		JMSConfiguration.setConfigPathDir(GeoserverDataDirectory
				.getGeoserverDataDirectory());
		// }
	}

	@Autowired
	@Qualifier("JMSPropertyPlaceholderConfigurer")
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
		return config.checkForOverride(BROKER_URL_KEY, DEFAULT_BROKER_URL);
		// if (!override) {
		// initDefaults(config);
		// override = true;
		// }
		// return override;
	}

}
