/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.server;

import it.geosolutions.geoserver.jms.JMSApplicationListener;
import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.events.ToggleType;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

/**
 * JMS MASTER (Producer) Listener used to provide basic functionalities to the producer implementations
 * 
 * @see {@link JMSApplicationListener}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public abstract class JMSAbstractProducer extends JMSApplicationListener {

    @Autowired
    public JMSFactory jmsFactory;

    /**
     * @return the jmsTemplate
     */
    public final JmsTemplate getJmsTemplate() {
        final ConnectionFactory cf = jmsFactory.getConnectionFactory(config.getConfigurations());
        if (cf == null) {
            throw new IllegalStateException("Unable to load a connectionFactory");
        }
        return new JmsTemplate(cf);
    }

    public final Topic getTopic() {
        final Topic jmsTopic = jmsFactory.getTopic(config.getConfigurations());
        if (jmsTopic == null) {
            throw new IllegalStateException("Unable to load a JMS destination");
        }
        return jmsTopic;
    }

    /**
     * Constructor
     * 
     * @param topicTemplate the getJmsTemplate() object used to send message to the topic queue
     * 
     */
    public JMSAbstractProducer() {
        super(ToggleType.MASTER);
    }
}
