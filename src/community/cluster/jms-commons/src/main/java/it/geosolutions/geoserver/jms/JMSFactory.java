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

import jj2000.j2k.NotImplementedError;

import org.springframework.beans.factory.DisposableBean;

/**
 * 
 * Implement this interface to add a new implementation to the application.<br/>
 * Note that only one implementation at time is permitted.
 * 
 * @author carlo cancellieri - geosolutions SAS
 * 
 */
public abstract class JMSFactory implements DisposableBean {

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
     * Starts an embedded broker
     * 
     * @param configuration
     * @throws Exception
     */
    public boolean startEmbeddedBroker(Properties configuration) throws Exception {
        throw new NotImplementedError("This functionality is not implemented");
    }

    /**
     * check the status of the embedded broker
     * 
     * @param configuration
     * @return
     */
    public boolean isEmbeddedBrokerStarted() {
        return false;
    }

    /**
     * Stops the embedded broker
     * 
     * @throws Exception
     */
    public boolean stopEmbeddedBroker() throws Exception {
        throw new NotImplementedError("This functionality is not implemented");
    }

}
