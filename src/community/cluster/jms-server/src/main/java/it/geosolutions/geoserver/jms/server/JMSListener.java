/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.server;

import it.geosolutions.geoserver.jms.events.ToggleEvent;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.geoserver.platform.ContextLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class JMSListener implements ApplicationListener<ApplicationEvent> {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(JMSConfigurationListener.class);

	// private final JMSProperties properties;
	// private final JmsTemplate jmsTemplate;

	@Autowired
	@Qualifier("serverDestination")
	public Destination jmsDestination;

	@Autowired
	public ConnectionFactory connectionFactory;
	/**
	 * This will be set to false:<br/>
	 * - until the GeoServer context is initialized<br/>
	 * - if this instance of geoserver act as pure slave
	 */
	private volatile Boolean producerEnabled = true;


	/**
	 * @return the jmsTemplate
	 */
	public final JmsTemplate getJmsTemplate() {
		// return jmsTemplate;
		if (connectionFactory == null) {
			throw new IllegalStateException(
					"Unable to load a connectionFactory");
		}

		return new JmsTemplate(connectionFactory);
	}

	public final Destination getDestination() {
		if (jmsDestination == null) {
			throw new IllegalStateException("Unable to load a JMS destination");
		}
		return jmsDestination;
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
				LOGGER.info("Setting JMS Catalog event publisher status to: "
						+ producerEnabled);
			}
			setProducerEnabled(producerEnabled);

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