package it.geosolutions.geoserver.jms.message;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.core.MessageCreator;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @deprecated unused/untested
 */
public class JMSTextMessageCreator implements MessageCreator {
	private final String message;

	public JMSTextMessageCreator(final String text) {
		message = text;
	}

	@Override
	public Message createMessage(Session session) throws JMSException {
		TextMessage textMessage = session.createTextMessage(message);

		return textMessage;
	}
}