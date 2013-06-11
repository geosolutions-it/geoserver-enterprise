/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.events.ToggleProducer;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class JMSCatalogAddEventHandlerSPI extends JMSCatalogEventHandlerSPI{
	
	public JMSCatalogAddEventHandlerSPI(final int priority, Catalog cat, XStream xstream, ToggleProducer producer) {
		super(priority,cat,xstream,producer);
	}

	@Override
	public boolean canHandle(final Object event) {
		if (event instanceof CatalogAddEvent)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,CatalogEvent> createHandler() {
		return new JMSCatalogAddEventHandler(catalog,xstream,this.getClass(),producer);
	}


}
