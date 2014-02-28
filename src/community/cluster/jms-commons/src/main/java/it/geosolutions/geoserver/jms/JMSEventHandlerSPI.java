/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.io.Serializable;

/**
 * SPI class which is used by JMSManager to instantiate the relative handler.<br/>
 * Its implementations may be loaded into the Spring context as a singleton.<br/>
 * SPI bean id name MUST be the same as SPI SimpleClassName <br/>
 * 
 * 
 * @see {@link JMSEventHandler}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 * @param <S>
 *            ToggleType implementing Serializable
 * @param <O>
 *            ToggleType of the object to handle
 */
public abstract class JMSEventHandlerSPI<S extends Serializable, O> {
	/**
	 * The key of the property stored into message which tells the Handler used
	 * to serialize the message and the one which will be used to de-serialize
	 * and synchronize
	 */
	private final static String PROPERTY_KEY = "JMSEventHandlerSPI";

	/**
	 * Integer representing the priority of this handler:<br/>
	 * <p>
	 * <b>Lower</b> value means <b>higher</b> priority.
	 * </p>
	 */
	private final int priority;

	public JMSEventHandlerSPI(final int priority) {
		this.priority = priority;
	}

	/**
	 * @return the priority
	 */
	public final int getPriority() {
		return priority;
	}

	public static String getKeyName() {
		return PROPERTY_KEY;
	}

	public abstract boolean canHandle(final Object event);

	public abstract JMSEventHandler<S, O> createHandler();

}
