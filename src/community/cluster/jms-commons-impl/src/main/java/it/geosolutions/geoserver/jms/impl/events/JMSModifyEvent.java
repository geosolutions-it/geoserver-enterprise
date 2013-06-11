/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.events;

import java.io.Serializable;
import java.util.List;

/**
 * Class implementing a generic JMS Modify event.<br>
 * It is used to handle serialization of a modify event.<br> 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 * @param <S> a Serializable object
 */
public class JMSModifyEvent<S extends Serializable> {
	
	final List<String> propertyNames;
	final List<Object> oldValues;
	final List<Object> newValues;
	final S source;

	public JMSModifyEvent(final S source, final List<String> propertyNames, final List<Object> oldValues,
			final List<Object> newValues) {
		this.source=source;
		this.propertyNames=propertyNames;
		this.oldValues=oldValues;
		this.newValues=newValues;
	}
	
	
	/**
	 * @return the propertyNames
	 */
	public final List<String> getPropertyNames() {
		return propertyNames;
	}


	/**
	 * @return the oldValues
	 */
	public final List<Object> getOldValues() {
		return oldValues;
	}


	/**
	 * @return the newValues
	 */
	public final List<Object> getNewValues() {
		return newValues;
	}


	public S getSource(){
		return (S)source;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
