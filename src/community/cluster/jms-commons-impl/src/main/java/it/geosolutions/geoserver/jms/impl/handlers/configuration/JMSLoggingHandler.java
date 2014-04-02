/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.configuration;

import it.geosolutions.geoserver.jms.events.ToggleProducer;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.NullArgumentException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * JMS Handler is used to synchronize 
 * 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSLoggingHandler extends JMSConfigurationHandler<LoggingInfo> {
	private static final long serialVersionUID = -6421638425464046597L;

	final static Logger LOGGER = LoggerFactory
			.getLogger(JMSLoggingHandler.class);

	private final GeoServer geoServer;
	private final ToggleProducer producer;

	public JMSLoggingHandler(GeoServer geo, XStream xstream,
			Class clazz, ToggleProducer producer) {
		super(xstream, clazz);
		this.geoServer = geo;
		this.producer = producer;
	}
	
	@Override
	protected void omitFields(final XStream xstream){
		// omit not serializable fields
		// NOTHING TO DO
	}

	@Override
	public boolean synchronize(LoggingInfo info) throws Exception {
		if (info==null) {
			throw new NullArgumentException("Incoming object is null");
		}
		try {
			// LOCALIZE service
			final LoggingInfo localObject=geoServer.getLogging();
			// overwrite local object members with new incoming settings
			BeanUtils.copyProperties(localObject, info);
			
			// disable the message producer to avoid recursion
			producer.disable();
			
			// save the localized object
			geoServer.save(localObject);

		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(this.getClass()+" is unable to synchronize the incoming event: "+info);
			throw e;
		} finally {
			// enable message the producer
			producer.enable();
		}
		return true;

	}

}
