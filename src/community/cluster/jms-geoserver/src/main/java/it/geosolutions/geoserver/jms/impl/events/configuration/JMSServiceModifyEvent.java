/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.events.configuration;

import it.geosolutions.geoserver.jms.impl.events.JMSModifyEvent;
import it.geosolutions.geoserver.jms.impl.handlers.configuration.JMSServiceHandler;

import java.util.List;

import org.geoserver.config.ServiceInfo;

/**
 * 
 * This Class define a wrapper of the {@link JMSModifyEvent} class to define an
 * event which can be recognized by the {@link JMSServiceHandler} as ServiceInfo modified
 * events.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSServiceModifyEvent extends JMSModifyEvent<ServiceInfo> {

	public JMSServiceModifyEvent(final ServiceInfo source,
			final List<String> propertyNames, final List<Object> oldValues,
			final List<Object> newValues) {
		super(source, propertyNames, oldValues, newValues);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
