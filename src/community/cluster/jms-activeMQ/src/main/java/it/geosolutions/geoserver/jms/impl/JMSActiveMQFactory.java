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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSActiveMQFactory extends JMSFactory {

    private final static java.util.logging.Logger LOGGER = Logging
            .getLogger(JMSActiveMQFactory.class);

    @Autowired
    private JMSConfiguration config;

    // used to track changes to the configuration
    private String broker;

    private String topicName;

    private PooledConnectionFactory cf;

    private Topic topic;

    // times to test (connection)
    private static int max;

    // millisecs to wait between tests (connection)
    private static long maxWait;

    // embedded broker
    private BrokerService brokerService;

    private final static String BROKER_URL_KEY = "xbeanURL";

    private final static String DEFAULT_BROKER_URL = "./broker.xml";

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
    // <!-- <property name="brokerURL" value="${broker.url}" /> -->
    // <property name="brokerURL" value="tcp://localhost:61616" />
    // </bean>
    @Override
    public ConnectionFactory getConnectionFactory(Properties configuration) {
        final String configuredBroker = configuration
                .getProperty(BrokerConfiguration.BROKER_URL_KEY);
        if (cf == null) {
            // need to be initialized
            broker = configuredBroker;
            cf = new PooledConnectionFactory(broker);

        } else if (broker == null || !broker.equals(configuredBroker)) {
            // need to be reinitialized
            broker = configuredBroker;
            // clear pending connections
            try {
                destroy();
            } catch (Exception e) {
                // eat
            }
            // create a new connection
            cf = new PooledConnectionFactory(broker);
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

    @Override
    public boolean startEmbeddedBroker(final Properties configuration) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting the embedded broker");
        }
        if (brokerService == null) {
            String xBeanBroker = configuration.getProperty(BROKER_URL_KEY);
            if (xBeanBroker == null || xBeanBroker.isEmpty()) {
                xBeanBroker = DEFAULT_BROKER_URL;
                config.putConfiguration(BROKER_URL_KEY, xBeanBroker);
                config.storeConfig();
            }
            final XBeanBrokerFactory bf = new XBeanBrokerFactory();
            brokerService = bf.createBroker(new URI(xBeanBroker));
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("The embedded broker service already exists, probably it is already started");
            }
            if (brokerService.isStarted()) {    
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("SKIPPING: The embedded broker is already started");
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
                        LOGGER.info("Embedded broker is now started");
                    }
                    return true;
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Unable to start the embedded broker" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to start the embedded broker");
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
            LOGGER.info("Embedded broker is now stopped");
        }
        if (brokerService == null) {
            return true;
        }
        brokerService.stop();
        for (int i = -1; i < max; ++i) {
            try {
                if (!brokerService.isStarted()) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Embedded broker is now stopped");
                    }
                    brokerService = null;
                    return true;
                }
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info("Embedded broker is going to stop: waiting...");
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe("Unable to start the embedded broker" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to stop the embedded broker");
        }
        return false;
    }

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
                            LOGGER.severe("Unable to start the embedded broker, force status to disabled");
                        }

                        // change configuration status
                        config.putConfiguration(ConnectionConfiguration.CONNECTION_KEY,
                                ConnectionConfigurationStatus.disabled.toString());

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
                    LOGGER.warning("The broker seems to be already started");
                }
            }
        }
    }

}
