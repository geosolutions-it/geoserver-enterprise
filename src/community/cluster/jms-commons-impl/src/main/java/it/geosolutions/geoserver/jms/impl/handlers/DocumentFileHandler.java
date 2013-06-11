/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.FileHandler;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * XML file handler:<br>
 * This class can be used to handle small XML files using JDOM
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class DocumentFileHandler extends
		JMSEventHandlerImpl<String, DocumentFile> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6421638425464046597L;

	final static Logger LOGGER = LoggerFactory.getLogger(FileHandler.class);

	public DocumentFileHandler(XStream xstream, Class clazz) {
		super(xstream, clazz);
	}

	@Override
	public boolean synchronize(DocumentFile event) throws Exception {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(event.getPath());
			xstream.toXML(event.getBody(), fout);
			return true;
		} catch (IOException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
			throw e;
		} finally {
			IOUtils.closeQuietly(fout);
		}
	}

	@Override
	public String serialize(DocumentFile o) throws Exception {
		return xstream.toXML(o);
	}

	@Override
	public DocumentFile deserialize(String o) throws Exception {
		return (DocumentFile) xstream.fromXML(o);
	}

}
