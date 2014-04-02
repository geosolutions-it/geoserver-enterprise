/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers;

import it.geosolutions.geoserver.jms.JMSEventHandler;
import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Base implementation of the JMSEventHandler which leverages on the XStream
 * [de]serializer. 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 * @param <S> The resulting serializable class type
 * @param <O> The type of the object to Handle
 */
public abstract class JMSEventHandlerImpl<S extends Serializable, O> extends
		JMSEventHandler<S, O> {
	//
	private static final long serialVersionUID = 8208466391619901813L;

	private final static Logger LOGGER = LoggerFactory
			.getLogger(JMSEventHandlerImpl.class);
	
	// [de]serialized
	protected final XStream xstream;

	/**
	 * @param xstream an already initialized xstream
	 * @param clazz the SPI class which generate this kind of handler
	 */
	public JMSEventHandlerImpl(final XStream xstream,
			Class<JMSEventHandlerSPI<S, O>> clazz) {
		super(clazz);
		this.xstream = xstream;
	}

	
}
