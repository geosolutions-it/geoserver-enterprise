/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

/**
 * 
 * @author carlo cancellieri - geosolutions SAS
 * 
 */
public abstract class JMSFactory {

	public abstract Destination getClientDestination(Properties configuration);

	public abstract Destination getServerDestination(Properties configuration);

	public abstract ConnectionFactory getConnectionFactory(Properties configuration);

}
