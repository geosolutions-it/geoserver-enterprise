/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.JMSEventHandlerSPI;
import it.geosolutions.geoserver.jms.impl.handlers.JMSEventHandlerImpl;

import javax.jms.JMSException;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.impl.CatalogImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Abstract class which use Xstream as message serializer/de-serializer.
 * We extend this class to implementing synchronize method.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public abstract class JMSCatalogEventHandler extends
		JMSEventHandlerImpl<String, CatalogEvent> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8208466391619901813L;

	final static Logger LOGGER = LoggerFactory
			.getLogger(JMSCatalogEventHandler.class);

	public JMSCatalogEventHandler(final XStream xstream,
			Class<JMSEventHandlerSPI<String, CatalogEvent>> clazz) {
		super(xstream,clazz);
		// omit not serializable fields
		omitFields();
	}

	/**
	 * omit not serializable fields
	 * @see {@link XStream}
	 */
	private void omitFields() {
		// omit not serializable fields
		xstream.omitField(CatalogImpl.class, "listeners");
		xstream.omitField(CatalogImpl.class, "facade");
		xstream.omitField(CatalogImpl.class, "resourcePool");
		xstream.omitField(CatalogImpl.class, "resourceLoader");
	}

	@Override
	public String serialize(CatalogEvent  event) throws Exception {
		return xstream.toXML(event);
	}

	@Override
	public CatalogEvent deserialize(String s) throws Exception {
		
		final Object source= xstream.fromXML(s);
		if (source instanceof CatalogEvent) {
			final CatalogEvent ev = (CatalogEvent) source;
			if (LOGGER.isDebugEnabled()) {
				final CatalogInfo info = ev.getSource();
				LOGGER.debug("Incoming message event of type CatalogEvent: "
						+ info.getId());
			}
			return ev;
		} else {
			throw new JMSException("Unable to deserialize the following object:\n"+s);
		}

	}
}
