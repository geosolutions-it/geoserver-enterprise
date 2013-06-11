/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.io.Serializable;
import java.util.Properties;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMS SLAVE (Consumer)
 * 
 * Simple Class which shows how to define and use a general purpose synchronizer.
 * 
 * @see {@link JMSEventHandler}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSSynchronizer {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(JMSSynchronizer.class);
	
	public <S extends Serializable, O> void synchronize(final O event, final Properties props) throws JMSException, IllegalArgumentException {
		try {
			// try to get the handler from the spring context
			final JMSEventHandler<S,O> handler = JMSManager.getHandler(event);
			// if handler is not found 
			if (handler==null){
				throw new IllegalArgumentException("Unable to locate a valid handler for the incoming event: "+event);
			}
			// else try to synchronize event using the obtained handler			
			handler.synchronize(event);
		
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Unable to synchronize event: "+event+" locally");
			}
			final JMSException ex=new JMSException(e.getLocalizedMessage());
			ex.initCause(e);
			throw ex;
		}	
	}
}
