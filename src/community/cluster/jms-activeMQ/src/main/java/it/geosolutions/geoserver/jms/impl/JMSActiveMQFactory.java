/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.TopicConfiguration;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSActiveMQFactory extends JMSFactory {

    // used to track changes to the configuration
    private String broker;

    private String topicName;

    private ConnectionFactory cf;

    private Topic topic;
    
    // <bean id="JMSClientDestination"
    // class="org.apache.activemq.command.ActiveMQQueue">
    // <value="Consumer.${instance.name}.VirtualTopic.${topic.name}" />
    // </bean>
    @Override
    public Destination getClientDestination(Properties configuration) {
        StringBuilder builder = new StringBuilder("Consumer.");
        String instanceName = configuration.getProperty(JMSConfiguration.INSTANCE_NAME_KEY);
        String topicName = configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY);
        return new org.apache.activemq.command.ActiveMQQueue(builder.append(instanceName)
                .append(".").append(topicName).toString());
    }

    // <!-- DESTINATION -->
    // <!-- A Destination in ActiveMQ -->
    // <bean id="JMSServerDestination" class="org.apache.activemq.command.ActiveMQTopic">
    // <!-- <constructor-arg value="VirtualTopic.${topic.name}" /> -->
    // <constructor-arg value="VirtualTopic.>" />
    // </bean>
    @Override
    public Topic getTopic(Properties configuration) {
        // TODO move me to implementation jmsFactory implementation
        // if topicName is changed
        final String topicConfiguredName = configuration
                .getProperty(TopicConfiguration.TOPIC_NAME_KEY);
        if (topic == null || topicName.equals(topicConfiguredName)) {
            topicName = topicConfiguredName;
            topic = new org.apache.activemq.command.ActiveMQTopic(
                    configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY));
        }
        if (topic == null) {
            throw new IllegalStateException("Unable to load a JMS Topic destination");
        }
        return topic;
    }


    // <!-- A connection to ActiveMQ -->
    // <bean id="JMSConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
    // <!-- <property name="brokerURL" value="${broker.url}" /> -->
    // <property name="brokerURL" value="tcp://localhost:61616" />
    // </bean>
    @Override
    public ConnectionFactory getConnectionFactory(Properties configuration) {
        final String configuredBroker = configuration
                .getProperty(BrokerConfiguration.BROKER_URL_KEY);
        if (cf == null || broker.equals(configuredBroker)) {
            broker = configuredBroker;
            cf = new CachingConnectionFactory(new ActiveMQConnectionFactory(
                    configuration.getProperty(BrokerConfiguration.BROKER_URL_KEY)));
        }
        if (cf == null) {
            throw new IllegalStateException("Unable to load a JMS ConnectionFactory");
        }
        return cf;
    }
}
