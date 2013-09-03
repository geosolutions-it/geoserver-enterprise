/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.impl.handlers.DocumentFile;
import it.geosolutions.geoserver.jms.impl.handlers.DocumentFileHandlerSPI;

import org.geoserver.catalog.Catalog;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class JMSCatalogStylesFileHandlerSPI extends DocumentFileHandlerSPI {
	
	final Catalog catalog;
	final XStream xstream;
	
	public JMSCatalogStylesFileHandlerSPI(final int priority, Catalog cat, XStream xstream) {
		super(priority,xstream);
		this.catalog=cat;
		this.xstream=xstream;
	}

	@Override
	public boolean canHandle(final Object event) {
		if (event instanceof DocumentFile)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,DocumentFile> createHandler() {
		return new JMSCatalogStylesFileHandler(catalog,xstream,JMSCatalogStylesFileHandlerSPI.class);
	}


}
