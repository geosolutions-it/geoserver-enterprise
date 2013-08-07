/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.configuration;

import java.io.IOException;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfigurationExt;

/**
 * 
 * Abstract class to store and load configuration from global var or
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class TopicConfiguration implements JMSConfigurationExt {

	public static final String TOPIC_NAME_KEY = "topicName";
	public static final String DEFAULT_TOPIC_NAME = "VirtualTopic.>";

	@Override
	public void initDefaults(JMSConfiguration config) throws IOException {
		config.putConfiguration(TOPIC_NAME_KEY, DEFAULT_TOPIC_NAME);
	}

	@Override
	public boolean checkForOverride(JMSConfiguration config) throws IOException {
		return config.checkForOverride(TOPIC_NAME_KEY, DEFAULT_TOPIC_NAME);
	}

}
