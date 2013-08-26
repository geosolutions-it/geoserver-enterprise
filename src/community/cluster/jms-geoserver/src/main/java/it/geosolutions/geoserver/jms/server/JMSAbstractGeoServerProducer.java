/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.server;

import it.geosolutions.geoserver.jms.impl.events.RestDispatcherCallback;

import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.geoserver.platform.ContextLoadedEvent;
import org.restlet.data.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * JMS MASTER (Producer) Listener used to provide basic functionalities to the producer implementations
 * 
 * @see {@link JMSAbstractProducer}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public abstract class JMSAbstractGeoServerProducer extends JMSAbstractProducer {
    private final static Logger LOGGER = LoggerFactory
            .getLogger(JMSAbstractGeoServerProducer.class);

    public JMSAbstractGeoServerProducer() {
        super();
        // disable producer until the application receive the ContextLoadedEvent
        setStatus(false);
    }

    /**
     * This should be called before each message send to add options (coming form the dispatcher callback) to the message
     * 
     * @return a copy of the configuration object updated with others options coming from the RestDispatcherCallback<br/>
     *         TODO use also options coming from the the GUI DispatcherCallback
     */
    protected Properties updateProperties() {
        // append options
        final Properties options = new Properties();
        for (Entry<Object, Object> e:config.getConfigurations().entrySet()){
            options.put(e.getKey(), e.getValue());
        }
        // TODO not all options are needed: append only instance name when NOT debug mode
        
        // get options from rest callback
        final List<Parameter> p = RestDispatcherCallback.getParameters();
        if (p != null) {
            for (Parameter par : p) {
                options.put(par.getName(), par.getValue().toString());
            }
        }
        return options;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // event coming from the GeoServer application when the configuration
        // load process is complete
        if (event instanceof ContextLoadedEvent) {
            boolean status = getStatus(type, config);
            if (LOGGER.isInfoEnabled()) {
                if (status)
                    LOGGER.info("Activating JMS Catalog event publisher...");
                else
                    LOGGER.info("JMS Catalog event publisher is disabled by configuration...");
            }
            // restore the status to the configured
            setStatus(status);
        } else {
            super.onApplicationEvent(event);
        }
    }

}
