package it.geosolutions.geoserver.jms.message;

import it.geosolutions.geoserver.jms.JMSProperties;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.springframework.jms.core.MessageCreator;

/**
 * Class implementing a MessageCreator which is used to produce valid
 * ObjectMessages
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 * @param <S>
 *            serializable class
 * @param <O>
 *            object to serialize
 */
public class JMSObjectMessageCreator implements
		MessageCreator {

	private final Serializable serialized;
	private final JMSProperties properties;

	public JMSObjectMessageCreator(final Serializable serialized, final JMSProperties props) {
		this.serialized = serialized;
		this.properties = props;
	}
	
	protected void updateProperties(Message message) throws JMSException{
		// append the name of the server
		message.setObjectProperty(properties.getKeyName(),properties.getName());
		
		// set other properties
		final Set<Entry<Object, Object>> set = properties.getProperties().entrySet();
		final Iterator<Entry<Object, Object>> it = set.iterator();
		while (it.hasNext()) {
			final Entry<Object, Object> entry = it.next();
			message.setObjectProperty(entry.getKey().toString(),
					entry.getValue());
		}
	}

	@Override
	public Message createMessage(Session session) throws JMSException {

		ObjectMessage message;
		try {
			message = session.createObjectMessage(serialized);
		} catch (Exception e) {
			final JMSException ex = new JMSException(
					e.getLocalizedMessage());
			ex.initCause(e);
			throw ex;
		}

//		// set the used SPI
//		message.setStringProperty(JMSEventHandlerSPI.getKeyName(),
//				handlerGenerator.getSimpleName());

		// append properties
		updateProperties(message);
		
		return message;
	}
}