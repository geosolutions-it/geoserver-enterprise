package it.geosolutions.geoserver.jms.events;

import org.springframework.context.ApplicationEvent;

/**
 * event defining the new state of the producer
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class ToggleEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	public ToggleEvent(Boolean source) {
		super(source);
	}

	public boolean toggleTo() {
		return (Boolean) getSource();
	}

}