package it.geosolutions.geoserver.jms.server;

import it.geosolutions.geoserver.jms.JMSProperties;
import it.geosolutions.geoserver.jms.JMSPublisher;
import it.geosolutions.geoserver.jms.events.ToggleProducer.ToggleEvent;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jms.core.JmsTemplate;

/**
 * JMS MASTER (Server)
 * 
 * This is a simple example of EventListener
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public abstract class JMSEventListener<S extends Serializable, O> implements ApplicationListener {

	private final static Logger LOGGER = LoggerFactory.getLogger(JMSEventListener.class);

	@Autowired
	private final JmsTemplate jmsTemplate;
	
	private final JMSProperties properties;

	/**
	 * @return the jmsTemplate
	 */
	public final JmsTemplate getJmsTemplate() {
		return jmsTemplate;
	}

	/**
	 * Constructor
	 * 
	 * @param topicTemplate
	 *            the JmsTemplate object used to send message to the topic queue
	 * 
	 */
	public JMSEventListener(final JmsTemplate topicTemplate, final JMSProperties props) {
		jmsTemplate = topicTemplate;
		this.properties=props;
	}
	
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		
		if (event instanceof ToggleEvent){
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Activating JMS event publisher...");
			}
			//TODO
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Incoming application event of type "+event.getClass().getSimpleName());
			}
		}
	}

	public void onEvent(O event) throws Exception {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Publishing event: "+event);
		
		final JMSPublisher publisher=new JMSPublisher();
		publisher.publish(jmsTemplate, properties, event);
	}

}
