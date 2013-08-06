/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.client;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

//<!-- and this is the message listener container -->
//<bean id="JMSContainer"
//	class="org.springframework.jms.listener.DefaultMessageListenerContainer">
//
//	<property name="concurrentConsumers" value="1" />
//	<property name="maxConcurrentConsumers" value="1" />
//
//	<property name="connectionFactory" ref="JMSConnectionFactory" />
//	<property name="destination" ref="JMSClientDestination" />
//	<property name="messageListener" ref="JMSQueueListener" />
//</bean>
final class JMSContainer extends DefaultMessageListenerContainer {

	@Autowired
	public JMSFactory jmsFactory;
	
	@Autowired
	public JMSConfiguration config;
	
	private boolean verified=false;

	private static void verify(final Object type, final String message) {
		if (type == null)
			throw new IllegalArgumentException(message != null ? message
					: "Verify fails the argument check");
	}

	@Override
	public void start() throws JmsException {
		if (!verified){
			verify(jmsFactory, "failed to get a JMSFactory");
			verify(config, "configuration is null");
			verified=true;
		}
		
		final Properties conf=config.getConfigurations();
		setDestination(jmsFactory.getClientDestination(conf));
		setConnectionFactory(jmsFactory.getConnectionFactory(conf));
		
		super.start();
	}

}
