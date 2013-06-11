/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;
import it.geosolutions.geoserver.jms.events.ToggleProducer;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogEvent;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public abstract class JMSCatalogEventHandlerSPI extends
		JMSEventHandlerSPI<String, CatalogEvent> {

	protected final Catalog catalog;
	protected final XStream xstream;
	protected final ToggleProducer producer;
	
	public JMSCatalogEventHandlerSPI(int priority, Catalog catalog,
			XStream xstream, ToggleProducer producer) {
		super(priority);
		this.catalog = catalog;
		this.xstream = xstream;
		this.producer = producer;
	}



}