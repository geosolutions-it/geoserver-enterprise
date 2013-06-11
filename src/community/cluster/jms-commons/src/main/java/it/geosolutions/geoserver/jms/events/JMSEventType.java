/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.events;

import java.io.Serializable;
import java.util.EventObject;

/**
 * Defining Event type example, each event points (source) to an object which represents
 * the incoming event.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public abstract class JMSEventType<S extends Serializable> extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8413744049417938375L;

	/**
	 * {@link EventObject#EventObject(Object)}
	 */
	public JMSEventType(S source) {
		super(source);
	}

	@Override
	public S getSource() {
		return getSource();
	}

	// the key of the property stored into message
	private final static String PROPERTY_KEY = "JMSEventType";

	public static String getKeyName() {
		return PROPERTY_KEY;
	}

	// the name of the type of the represented message
	public String getName() {
		return this.getClass().getSimpleName();
	}
}
