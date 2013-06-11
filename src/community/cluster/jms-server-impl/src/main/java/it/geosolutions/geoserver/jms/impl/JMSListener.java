/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl;

import it.geosolutions.geoserver.jms.JMSProperties;
import it.geosolutions.geoserver.jms.events.ToggleProducer.ToggleEvent;

import org.geoserver.platform.ContextLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jms.core.JmsTemplate;

/**
 * JMS Master (Producer)
 * 
 * This class make it possible to enable and disable the Event producer using
 * Applications Events.
 * 
 * This is used at the GeoServer startup to disable the producer until the
 * initial configuration is loaded.
 * 
 * It can also be used to enable/disable the producer in a Master+Slave
 * configuration to avoid recursive event production.
 * 
 * @see {@link ToggleEvent}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSListener implements ApplicationListener {

	final static Logger LOGGER = LoggerFactory
			.getLogger(JMSConfigurationListener.class);

	private final JMSProperties properties;
	private final JmsTemplate jmsTemplate;

	/**
	 * this will be set to false: - until the GeoServer context is initialized -
	 * if this instance of geoserver act as pure slave
	 */
	private volatile Boolean producerEnabled = false;

	public JMSListener(JMSProperties properties, JmsTemplate jmsTemplate) {
		super();
		this.properties = properties;
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * @return the jmsTemplate
	 */
	public final JmsTemplate getJmsTemplate() {
		return jmsTemplate;
	}

	/**
	 * @return the properties
	 */
	public JMSProperties getProperties() {
		return properties;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Incoming event of type "
					+ event.getClass().getSimpleName());
		}

		// event coming from the GeoServer application when the configuration
		// load process is complete
		if (event instanceof ContextLoadedEvent) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Activating JMS Catalog event publisher...");
			}
			setProducerEnabled(true);

		} else if (event instanceof ToggleEvent) {

			// enable/disable the producer
			final ToggleEvent tEv = (ToggleEvent) event;
			setProducerEnabled(tEv.toggleTo());

		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Incoming application event of type "
						+ event.getClass().getSimpleName());
			}
		}
	}

	/**
	 * @return the producerEnabled
	 */
	public final boolean isProducerEnabled() {
		return producerEnabled;
	}

	/**
	 * @param producerEnabled
	 *            enable or disable producer
	 * @note thread safe
	 */
	public final void setProducerEnabled(final boolean producerEnabled) {
		if (producerEnabled) {
			// if produce is disable -> enable it
			if (!this.producerEnabled) {
				synchronized (this.producerEnabled) {
					if (!this.producerEnabled) {
						this.producerEnabled = true;
					}
				}
			}
		} else {
			// if produce is Enabled -> disable
			if (this.producerEnabled) {
				synchronized (this.producerEnabled) {
					if (this.producerEnabled) {
						this.producerEnabled = false;
					}
				}
			}

		}
	}

}