/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import it.geosolutions.geoserver.jms.configuration.EmbeddedBrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.TopicConfiguration;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Topic;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.xbean.XBeanBrokerFactory;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSActiveMQFactory extends JMSFactory implements InitializingBean {

    private final static java.util.logging.Logger LOGGER = Logging
            .getLogger(JMSActiveMQFactory.class);

    @Autowired
    private JMSConfiguration config;

    // used to track changes to the configuration
    private String brokerURI;

    private String topicName;

    private PooledConnectionFactory cf;

    private Topic topic;

    // times to test (connection)
    private static int max;

    // millisecs to wait between tests (connection)
    private static long maxWait;

    // embedded brokerURI
    private BrokerService brokerService;

    private String brokerName;
    
    @Override
    public void afterPropertiesSet() throws Exception {
    	// TODO initialize connections in order (first broker, then client)
    }

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
    // <bean id="JMSServerDestination"
    // class="org.apache.activemq.command.ActiveMQTopic">
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
    // <bean id="JMSConnectionFactory"
    // class="org.apache.activemq.ActiveMQConnectionFactory">
    // <!-- <property name="brokerURL" value="${brokerURI.url}" /> -->
    // <property name="brokerURL" value="tcp://localhost:61616" />
    // </bean>
    @Override
    public ConnectionFactory getConnectionFactory(Properties configuration) {
        String _brokerURI = configuration.getProperty(BrokerConfiguration.BROKER_URL_KEY);
        if(isEmbeddedBrokerStarted()&&(_brokerURI==null||_brokerURI.length()==0)){
        	brokerURI="vm://"+brokerName+"?create=false&waitForStart=5000";
        }
        
        if (cf == null) {
            // need to be initialized
            cf = new PooledConnectionFactory(brokerURI);

        } else if (brokerURI == null || !brokerURI.equals(_brokerURI)) {
            // clear pending connections
            try {
                destroy();
            } catch (Exception e) {
                // eat
            }
            // create a new connection
            cf = new PooledConnectionFactory(brokerURI);
            // cf.start();
        }
        return cf;
    }

    @Override
    public void destroy() throws Exception {
        if (cf != null) {
            // close all the connections
            cf.clear();
            // stop the factory
            cf.stop();
            // set null
            cf = null;
        }
        if (brokerService != null) {
            brokerService.stop();
        }
    }
    
    @Autowired
    JMSXBeanBrokerFactory bf;

    @Override
    public boolean startEmbeddedBroker(final Properties configuration) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting the embedded brokerURI");
        }
        if (brokerService == null) {
            final String xBeanBroker = configuration.getProperty(ActiveMQEmbeddedBrokerConfiguration.BROKER_URL_KEY);
//            final XBeanBrokerFactory bf = new XBeanBrokerFactory();
            brokerService = bf.createBroker(new URI(xBeanBroker));
            brokerService.setEnableStatistics(false);
            
            // override the name of the brokerURI using the instance name which should be unique within the network
            brokerName = configuration.getProperty(JMSConfiguration.INSTANCE_NAME_KEY);
            brokerService.setBrokerName(brokerName);
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("The embedded brokerURI service already exists, probably it is already started");
            }
            if (brokerService.isStarted()) {    
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("SKIPPING: The embedded brokerURI is already started");
                }
                return true;
            }
        }
        if (!brokerService.isStarted()) {
            brokerService.start();
        }
        for (int i = -1; i < max; ++i) {
            try {
                if (brokerService.isStarted()) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Embedded brokerURI is now started");
                    }
                    return true;
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Unable to start the embedded brokerURI" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to start the embedded brokerURI");
        }
        return false;
    }

    @Override
    public boolean isEmbeddedBrokerStarted() {
        return brokerService == null ? false : brokerService.isStarted();
    }

    @Override
    public boolean stopEmbeddedBroker() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Embedded brokerURI is now stopped");
        }
        if (brokerService == null) {
            return true;
        }
        brokerService.stop();
        for (int i = -1; i < max; ++i) {
            try {
                if (!brokerService.isStarted()) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Embedded brokerURI is now stopped");
                    }
                    brokerService = null;
                    return true;
                }
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info("Embedded brokerURI is going to stop: waiting...");
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Unable to start the embedded brokerURI" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to stop the embedded brokerURI");
        }
        return false;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void init() {
        // // times to test (connection)
        max = Integer.parseInt(config
                .getConfiguration(ConnectionConfiguration.CONNECTION_RETRY_KEY).toString());
        // millisecs to wait between tests (connection)
        maxWait = Long.parseLong(config.getConfiguration(
                ConnectionConfiguration.CONNECTION_MAXWAIT_KEY).toString());

        // check configuration for connection and try to start if needed
        if (EmbeddedBrokerConfiguration.isEnabled(config)) {
            if (!isEmbeddedBrokerStarted()) {
                try {
                    if (!startEmbeddedBroker(config.getConfigurations())) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.severe("Unable to start the embedded brokerURI, force status to disabled");
                        }

                        // change configuration status
                        config.putConfiguration(ConnectionConfiguration.CONNECTION_KEY,
                                ConnectionConfigurationStatus.disabled.toString());

                    }else{
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.severe("Started the embedded brokerURI: "+brokerService.toString());
                        }                        
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    // change configuration status
                    config.putConfiguration(ConnectionConfiguration.CONNECTION_KEY,
                            ConnectionConfigurationStatus.disabled.toString());
                }
                // store changes to the configuration
                try {
                    config.storeConfig();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("The brokerURI seems to be already started");
                }
            }
        }
    }

}
