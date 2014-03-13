/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms;

import it.geosolutions.geoserver.jms.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.TopicConfiguration;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Topic;

/**
 * 
 * Implement this interface to add a new implementation to the application.<br/>
 * Note that only one implementation at time is permitted.
 * 
 * @author carlo cancellieri - geosolutions SAS
 * 
 */
public abstract class JMSFactory {

    /**
     * Must return a {@link Destination} configured with the passed property.<br/>
     * You may leverage on {@link JMSConfiguration#INSTANCE_NAME_KEY}
     * 
     * @param configuration
     * @return a valid destination pointing to a temporary queue to use for responses
     */
    public abstract Destination getClientDestination(Properties configuration);

    /**
     * Must return a {@link Destination} configured with the passed property.<br/>
     * You may leverage on {@link BrokerConfiguration} or {@link ConnectionConfiguration}
     * 
     * @param configuration
     * @return a valid Topic
     */
    public abstract Topic getTopic(Properties configuration);

    /**
     * Must return a {@link ConnectionFactory} configured with the passed property.<br/>
     * You may leverage on {@link TopicConfiguration} or {@link ConnectionConfiguration}
     * 
     * @param configuration
     * @return a ConnectionFactory
     */
    public abstract ConnectionFactory getConnectionFactory(Properties configuration);

    /**
     * This is called when the JMSContainer is disposed, can be used to shutdown used resources.
     */
    public abstract void shutdown();

}
