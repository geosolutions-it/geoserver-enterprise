/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.configuration;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;
import it.geosolutions.geoserver.jms.events.ToggleProducer;

import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

import com.thoughtworks.xstream.XStream;


public class JMSLoggingHandlerSPI extends JMSEventHandlerSPI<String,LoggingInfo> {
	
	final GeoServer geoserver;
	final XStream xstream;
	final ToggleProducer producer;
	
	public JMSLoggingHandlerSPI(final int priority, final GeoServer geo, final XStream xstream, final ToggleProducer producer) {
		super(priority);
		this.geoserver=geo;
		this.xstream=xstream;
		this.producer=producer;
	}

	@Override
	public boolean canHandle(final Object event) {
		if (event instanceof LoggingInfo)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,LoggingInfo> createHandler() {
		return new JMSLoggingHandler(geoserver,xstream,this.getClass(),producer);
	}


}
