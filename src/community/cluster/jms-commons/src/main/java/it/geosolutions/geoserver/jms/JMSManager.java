/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class used to handle JMS extensions. Here we define a set of functions to
 * perform resource lookup into the Spring context.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSManager implements ApplicationContextAware, InitializingBean {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(JMSManager.class);

	private ApplicationContext applicationContext = null;

	private Map<String, ?> beans;

	private static JMSManager singleton;

	private JMSManager() {
	};

	@Override
	public void afterPropertiesSet() throws Exception {
		if (applicationContext == null)
			throw new IllegalStateException(
					"The provided applicationContext is null!");

		singleton = this;

		this.beans = applicationContext
				.getBeansOfType(JMSEventHandlerSPI.class);

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public static <S extends Serializable, O> JMSEventHandler<S, O> getHandler(
			final O event) throws IllegalArgumentException {
		final JMSEventHandler<S, O> handler = singleton.loadHandler(event);
		return handler;
	}

	/**
	 * Method to make lookup using the type of the passed eventType.
	 * 
	 * @param <S>
	 * @param <O>
	 * @param eventType
	 * @return the handler
	 * @throws IllegalArgumentException
	 */
	private <S extends Serializable, O> JMSEventHandler<S, O> loadHandler(
			final O eventType) throws IllegalArgumentException {
		final Set<?> beanSet = beans.entrySet();
		// declare a tree set to define the handler priority
		final Set<JMSEventHandlerSPI<S, O>> candidates = new TreeSet<JMSEventHandlerSPI<S, O>>(
				new Comparator<JMSEventHandlerSPI<S, O>>() {
					@Override
					public int compare(JMSEventHandlerSPI<S, O> o1,
							JMSEventHandlerSPI<S, O> o2) {
						if (o1.getPriority() < o2.getPriority())
							return -1;
						else if (o1.getPriority() == o2.getPriority()) {
							return 0;
							// } else if (o1.getPriority()>o2.getPriority()){
						} else {
							return 1;
						}
					}
				});
		// for each handler check if it 'canHandle' the incoming object if so
		// add it to the tree
		for (final Iterator<?> it = beanSet.iterator(); it.hasNext();) {
			final Map.Entry<String, ?> entry = (Entry<String, ?>) it.next();

			final JMSEventHandlerSPI<S, O> spi = (JMSEventHandlerSPI) entry
					.getValue();
			if (spi != null) {
				if (spi.canHandle(eventType)) {
					if (LOGGER.isInfoEnabled())
						LOGGER.info("Creating an instance of: "
								+ spi.getClass());
					candidates.add(spi);
				}
			}
		}
		// TODO return the entire tree leaving choice to the caller (useful to
		// build a failover list)
		// return the first available handler
		final Iterator<JMSEventHandlerSPI<S, O>> it = candidates.iterator();
		while (it.hasNext()) {
			try {
				final JMSEventHandler<S, O> handler = it.next().createHandler();
				if (handler != null)
					return handler;
			} catch (Exception e) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn(e.getLocalizedMessage(), e);
			}

		}
		final String message = "Unable to find the needed Handler SPI for event of type: "
				+ eventType.getClass().getCanonicalName();
		if (LOGGER.isWarnEnabled())
			LOGGER.warn(message);
		throw new IllegalArgumentException(message);
	}

	public static <S extends Serializable, O> JMSEventHandler<S, O> getHandlerByClassName(
			final String clazz) throws IllegalArgumentException {
		final JMSEventHandler<S, O> handler = singleton
				.loadHandlerByClassName(clazz);
		return handler;
	}

	private <S extends Serializable, O> JMSEventHandler<S, O> loadHandlerByClassName(
			final String clazzName) throws IllegalArgumentException {
		final Object spiBean = beans.get(clazzName);
		if (spiBean != null) {
			JMSEventHandlerSPI<S, O> spi = JMSEventHandlerSPI.class
					.cast(spiBean);
			if (spi != null) {
				return spi.createHandler();
			}
		}

		final String message = "Unable to find the Handler SPI called: "
				+ clazzName;
		if (LOGGER.isWarnEnabled())
			LOGGER.warn(message);
		throw new IllegalArgumentException(message);
	}
}
