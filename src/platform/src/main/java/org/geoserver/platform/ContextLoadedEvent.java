package org.geoserver.platform;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * Custom application context event that is fired after the application context is 
 * successfully loaded.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ContextLoadedEvent extends ApplicationContextEvent {

    public ContextLoadedEvent(ApplicationContext source) {
        super(source);
    }

}
