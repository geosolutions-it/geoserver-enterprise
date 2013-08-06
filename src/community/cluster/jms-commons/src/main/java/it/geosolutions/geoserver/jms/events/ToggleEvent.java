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
	
	private final ToggleType toggleType;
	
	public ToggleEvent(Boolean source, ToggleType toggleType) {
		super(source);
		this.toggleType=toggleType;
	}
	
	public ToggleType getType(){
		return this.toggleType;
	}

	public boolean toggleTo() {
		return (Boolean) getSource();
	}

}