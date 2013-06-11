/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.io.Serializable;
import java.util.Properties;

/**
 * 
 * <p>
 * An handler is an extension class for the JMS platform which define a set of
 * basic operations:
 * </p>
 * <ul>
 * <li><b>serialize:</b> {@link JMSEventHandler#serialize(Object)}</li>
 * <li><b>deserialize:</b> {@link JMSEventHandler#deserialize(Serializable)}</li>
 * <li><b>synchronize:</b> {@link JMSEventHandler#synchronize(Object)}</li>
 * </ul>
 * 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 * @param <S>
 *            type implementing Serializable
 * @param <O>
 *            the type of the object this handler is able to handle
 */
public abstract class JMSEventHandler<S extends Serializable, O> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3475796618825492441L;

	private final Class<JMSEventHandlerSPI<S, O>> generatorClass;
	
	private Properties properties;
	
	public Properties getProperties(){
		return properties;
	}
	
	public void setProperties(final Properties properties){
		this.properties=properties;
	}

	public JMSEventHandler(Class<JMSEventHandlerSPI<S, O>> generatorClazz) {
		this.generatorClass = generatorClazz;
	}

	/**
	 * @return the generatorClass
	 */
	public final Class<JMSEventHandlerSPI<S, O>> getGeneratorClass() {
		return generatorClass;
	}

	/**
	 * Its scope is to serialize from an object of type <O> to instance of a
	 * Serializable object.<br>
	 * That instance will be used by the {@link JMSPublisher} to send the object
	 * over a JMS topic.<br>
	 * <p>
	 * This method is used exclusively on the Server side.
	 * </p>
	 * 
	 * @param o
	 *            the object of type <O> to serialize
	 * @return a serializable object
	 * @throws Exception
	 */
	public abstract S serialize(O o) throws Exception;

	/**
	 * Its scope is to create a new instance of type <O> de-serializing the
	 * object of type <S>.<br>
	 * That instance will be used by the {@link JMSSynchronizer} to obtain (from
	 * the JMS topic) an instance to pass to the synchronize method (
	 * {@link #synchronize(Object)}).<br>
	 * <p>
	 * This method is used exclusively on the Client side
	 * </p>
	 * 
	 * @param o
	 *            the object of type <O> to serialize
	 * @return a serializable object
	 * @throws Exception
	 */
	public abstract O deserialize(S o) throws Exception;

	/**
	 * Its scope is to do something with the deserialized
	 * {@link #deserialize(Serializable)} object.
	 * <p>
	 * This method is used exclusively on the Client side
	 * </p>
	 * 
	 * @param deserialized the deserialized object 
	 * @return a boolean true if the operation ends successfully false otherwise
	 * @throws Exception if something goes wrong
	 */
	public abstract boolean synchronize(O deserialized) throws Exception;

}
