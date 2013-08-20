/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.client;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

final public class JMSContainer extends DefaultMessageListenerContainer {

	@Autowired
	public JMSFactory jmsFactory;

	@Autowired
	public List<JMSContainerHandlerExceptionListener> jmsContainerHandleExceptionListener;

	private JMSConfiguration config;
	//
	// private JMSQueueListener listener;

	private boolean verified = false;

	public JMSContainer(JMSConfiguration config, JMSQueueListener listener) {
		super();

		// the listener used to handle incoming events
		setMessageListener(listener);
		// configuration
		this.config = config;

		// TODO ad-hoc config
//		final String startString = config
//				.getConfiguration(ConnectionConfiguration.TOGGLE_SLAVE_KEY);
//		if (startString != null) {
//			setAutoStartup(Boolean.parseBoolean(startString));
//		} else {
			setAutoStartup(false);
//		}
	}

	@PostConstruct
	private void init() {
		final Properties conf = config.getConfigurations();
		setDestination(jmsFactory.getClientDestination(conf));
		setConnectionFactory(jmsFactory.getConnectionFactory(conf));
	}

	private static void verify(final Object type, final String message) {
		if (type == null)
			throw new IllegalArgumentException(message != null ? message
					: "Verify fails the argument check");
	}

	@Override
	public void start() throws JmsException {
		if (!verified) {
			verify(jmsFactory, "failed to get a JMSFactory");
			verified = true;
		}
		if (!isRunning()) {
			init();
			super.start();
		}
	}

	@Override
	protected void handleListenerSetupFailure(Throwable ex,
			boolean alreadyRecovered) {
		super.handleListenerSetupFailure(ex, alreadyRecovered);

		if (jmsContainerHandleExceptionListener != null) {
			for (JMSContainerHandlerExceptionListener handler : jmsContainerHandleExceptionListener) {
				handler.handleListenerSetupFailure(ex, alreadyRecovered);
			}
		}
	}
}
