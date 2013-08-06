/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.configuration;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;
import it.geosolutions.geoserver.jms.events.ToggleProducer;
import it.geosolutions.geoserver.jms.impl.events.configuration.JMSServiceModifyEvent;

import org.geoserver.config.GeoServer;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class JMSServiceHandlerSPI extends JMSEventHandlerSPI<String,JMSServiceModifyEvent> {
	
	private final GeoServer geoserver;
	private final XStream xstream;
	private final ToggleProducer producer;
	
	public JMSServiceHandlerSPI(final int priority, final GeoServer geo, final XStream xstream, final ToggleProducer producer) {
		super(priority);
		this.geoserver=geo;
		this.xstream=xstream;
		this.producer=producer;
	}

	@Override
	public boolean canHandle(final Object event) {
//		if (event instanceof ServiceInfo)
//			return true;
//		else 
		if (event instanceof JMSServiceModifyEvent)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,JMSServiceModifyEvent> createHandler() {
		return new JMSServiceHandler(geoserver,xstream,this.getClass(),producer);
	}


}
