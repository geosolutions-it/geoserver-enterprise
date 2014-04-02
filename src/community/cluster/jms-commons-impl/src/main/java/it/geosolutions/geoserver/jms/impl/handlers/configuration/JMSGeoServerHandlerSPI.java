/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.configuration;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;
import it.geosolutions.geoserver.jms.events.ToggleProducer;
import it.geosolutions.geoserver.jms.impl.events.configuration.JMSGlobalModifyEvent;

import org.geoserver.config.GeoServer;

import com.thoughtworks.xstream.XStream;

public class JMSGeoServerHandlerSPI extends JMSEventHandlerSPI<String,JMSGlobalModifyEvent> {
	
	final GeoServer geoserver;
	final XStream xstream;
	final ToggleProducer producer;
	
	public JMSGeoServerHandlerSPI(final int priority, final GeoServer geo, final XStream xstream, final ToggleProducer producer) {
		super(priority);
		this.geoserver=geo;
		this.xstream=xstream;
		this.producer=producer;
	}

	@Override
	public boolean canHandle(final Object event) {
		if (event instanceof JMSGlobalModifyEvent)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,JMSGlobalModifyEvent> createHandler() {
		return new JMSGeoServerHandler(geoserver,xstream,this.getClass(),producer);
	}


}
