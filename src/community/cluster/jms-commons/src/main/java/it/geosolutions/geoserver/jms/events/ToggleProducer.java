/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.events;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * An instance of this class can be used to send over the Application Context
 * ToggleEvent events. Those events can be used by the a producer to enable or
 * disable the message events production over the JMS channel.
 * 
 * @see {@link JMSEventListener}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class ToggleProducer implements ApplicationContextAware {

	private ApplicationContext ctx;

	/**
	 * true if the toggle can run enable and disable publishing events, false
	 * otherwise
	 */
	private volatile Boolean toggleEnabled = true;

	public ToggleProducer() {

	}

	public ToggleProducer(final ApplicationContext ctx,
			final Boolean toggleEnabled) {
		super();
		this.ctx = ctx;
		this.toggleEnabled = toggleEnabled;
	}

	/**
	 * @param toggleEnabled
	 *            set enabled and disabled the toggle itself
	 */
	public final void setToggleEnabled(boolean toggleEnabled) {
		synchronized (this.toggleEnabled) {
			this.toggleEnabled = toggleEnabled;
		}
	}

	/**
	 * @return the true if the toggle can enable and disable, false otherwise
	 */
	public final boolean isToggleEnabled() {
		return toggleEnabled;
	}

	public void setApplicationContext(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	public void enable() {
		if (isToggleEnabled()) {
			ctx.publishEvent(new ToggleEvent(Boolean.TRUE));
		}
	}

	public void disable() {
		if (isToggleEnabled()) {
			ctx.publishEvent(new ToggleEvent(Boolean.FALSE));
		}
	}

}
