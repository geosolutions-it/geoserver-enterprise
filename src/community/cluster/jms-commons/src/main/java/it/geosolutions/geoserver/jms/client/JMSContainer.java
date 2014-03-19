/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.client;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * 
 * Connection handler
 * 
 * @author Carlo Cancellieri - GeoSolutions SAS
 * 
 */
final public class JMSContainer extends DefaultMessageListenerContainer implements DisposableBean {

    private final static Logger LOGGER = Logging.getLogger(JMSContainer.class);

    @Autowired
    public JMSFactory jmsFactory;

    @Autowired
    public List<JMSContainerHandlerExceptionListener> jmsContainerHandleExceptionListener;

    private JMSConfiguration config;

    private boolean verified = false;

    // times to test (connection)
    private static int max;

    // millisecs to wait between tests (connection)
    private static long maxWait;

    public JMSContainer(JMSConfiguration config, JMSQueueListener listener) {
        super();
        
        // force no concurrence
        setConcurrentConsumers(1);
        
        // the listener used to handle incoming events
        setMessageListener(listener);

        // configuration
        this.config = config;

    }

    @PostConstruct
    private void init() {
        // change the default autostartup status
        setAutoStartup(false);
        
        // times to test (connection)
        max = Integer.parseInt(config.getConfiguration(ConnectionConfiguration.CONNECTION_RETRY_KEY).toString());
        // millisecs to wait between tests (connection)
        maxWait = Long.parseLong(config.getConfiguration(ConnectionConfiguration.CONNECTION_MAXWAIT_KEY).toString());

        // check configuration for connection and try to start if needed
        final String startString = config.getConfiguration(ConnectionConfiguration.CONNECTION_KEY);
        if (startString != null
                && startString.equals(ConnectionConfigurationStatus.enabled.toString())) {
            if (!connect()) {
                if (LOGGER.isLoggable(Level.SEVERE)){
                    LOGGER.severe("Unable to connect to the broker, force connection status to disabled");
                }

                // change configuration status
                config.putConfiguration(ConnectionConfiguration.CONNECTION_KEY,
                        ConnectionConfigurationStatus.disabled.toString());

                // store changes to the configuration
                try {
                    config.storeConfig();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,e.getMessage(), e);
                }
            }
        } else {
            // configure (needed by initializeBean)
            configure();
        }
    }

    private static void verify(final Object type, final String message) {
        if (type == null)
            throw new IllegalArgumentException(message != null ? message
                    : "Verify fails the argument check");
    }

    /**
     * try to disconnect
     * 
     * @return true if success
     */
    public boolean disconnect() {
        if (isRunning()) {
            LOGGER.info("Disconnecting...");
            stop();
            for (int rep = 1; rep <= max; ++rep) {
                LOGGER.info("Unregistering...");
                if (!isRunning()) {
                    LOGGER.info("Succesfully un-registered from the destination topic");
                    LOGGER.warning("You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
                    return true;
                }
                LOGGER.info("Waiting for connection shutdown...(" + rep + "/" + max + ")");
                try {
                    Thread.sleep(maxWait);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE,e.getLocalizedMessage(), e);
                }
            }
        } else {
            LOGGER.severe("Connection is already stopped");
        }

        return false;
    }

    private void configure() {
        final Properties conf = config.getConfigurations();

        // set destination
        setDestination(jmsFactory.getClientDestination(conf));
        
        // use a CachingConnectionFactory
        setConnectionFactory(jmsFactory.getConnectionFactory(conf));
    }

    /**
     * try to connect
     * 
     * @return true in success case, false otherwise
     */
    public boolean connect() {
        if (!isRunning()) {
            LOGGER.info("Connecting...");
            start();
            if (isRunning()) {
                for (int repReg = 1; repReg <= max; ++repReg) {
                    LOGGER.info("Registration...");
                    if (isRegisteredWithDestination()) {
                        LOGGER.info("Now GeoServer is registered with the destination");
                        return true;
                    } else if (repReg == max) {
                        LOGGER.log(Level.SEVERE,"Registration aborted due to a connection problem");
                        stop();
                        LOGGER.info("Disconnected");
                    } else {
                        LOGGER.info("Impossible to register GeoServer with destination, waiting...");
                    }
                    LOGGER.info("Waiting for registration...(" + repReg + "/" + max + ")");
                    try {
                        Thread.sleep(maxWait);
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.SEVERE,e.getLocalizedMessage(), e);
                    }
                }
            } else {
                LOGGER.severe("Impossible to start a connection to destination.");
                stop();
                LOGGER.info("Disconnected");
                return false;
            }
        } else {
            LOGGER.severe("Connection is already running");
        }
        return false;
    }

    @Override
    public void start() throws JmsException {
        if (!verified) {
            verify(jmsFactory, "failed to get a JMSFactory");
            verified = true;
        }
        if (!isRunning()) {

            // configure the container
            configure();

            // start it
            super.start();

            // initialize the container
            initialize();

        }
    }
    
    @Override
    public void destroy() {
        super.stop();
        super.destroy();
    }

    @Override
    protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
        super.handleListenerSetupFailure(ex, alreadyRecovered);

        if (jmsContainerHandleExceptionListener != null) {
            for (JMSContainerHandlerExceptionListener handler : jmsContainerHandleExceptionListener) {
                handler.handleListenerSetupFailure(ex, alreadyRecovered);
            }
        }
    }
}
