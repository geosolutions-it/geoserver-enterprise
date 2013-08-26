/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.server;

import it.geosolutions.geoserver.jms.JMSApplicationListener;
import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.events.ToggleType;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
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

    // private ConnectionFactory cf;// use jmsTemplate.getConnectionFactory()

    private JmsTemplate jmsTemplate;

    private Destination jmsDestination;

    @PostConstruct
    private void init() {
        jmsDestination = jmsFactory.getServerDestination(config.getConfigurations());
        if (jmsDestination == null) {
            throw new IllegalStateException("Unable to load a JMS destination");
        }

        final ConnectionFactory cf = jmsFactory.getConnectionFactory(config.getConfigurations());
        if (cf == null) {
            throw new IllegalStateException("Unable to load a connectionFactory");
        }
        jmsTemplate = new JmsTemplate(new CachingConnectionFactory(cf));
        
    }

    /**
     * @return the jmsTemplate
     */
    public final JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public final Destination getDestination() {
        return jmsDestination;
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
