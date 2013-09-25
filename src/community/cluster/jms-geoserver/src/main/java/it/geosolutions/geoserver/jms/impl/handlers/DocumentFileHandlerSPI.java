/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;

import com.thoughtworks.xstream.XStream;

/**
 * Handler which is able to handle DocumentFile objects.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class DocumentFileHandlerSPI extends JMSEventHandlerSPI<String,DocumentFile> {
	
	final XStream xstream;
	
	public DocumentFileHandlerSPI(final int priority, final XStream xstream) {
		super(priority);
		this.xstream=xstream;
	}

	@Override
	public boolean canHandle(final Object jdom) {
		if (jdom instanceof DocumentFile)
			return true;
		else
			return false;
	}

	@Override
	public JMSEventHandler<String,DocumentFile> createHandler() {
		return new DocumentFileHandler(xstream,DocumentFileHandlerSPI.class);
	}
}
