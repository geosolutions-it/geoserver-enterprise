/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.Serializable;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
public class FeatureAttribute implements Serializable {

	private static final long serialVersionUID = 5280048102929539311L;
	private String name;
	private Object value;

	public FeatureAttribute(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public Object getName() {
		return name;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
}
