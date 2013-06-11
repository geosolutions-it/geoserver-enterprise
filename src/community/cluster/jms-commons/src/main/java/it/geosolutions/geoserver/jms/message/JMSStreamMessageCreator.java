package it.geosolutions.geoserver.jms.message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.StreamMessage;

import org.apache.commons.io.IOUtils;
import org.springframework.jms.core.MessageCreator;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @deprecated unused/untested
 */
public class JMSStreamMessageCreator implements MessageCreator {
	private final File file;

	public JMSStreamMessageCreator(final File file) {
		this.file = file;
	}

	@Override
	public Message createMessage(Session session) throws JMSException {
		final StreamMessage message = session.createStreamMessage();

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			final int size = 1024;
			final byte[] buf = new byte[size];
			int read = 0;
			while ((read = fis.read(buf)) != -1) {
				message.writeBytes(buf, 0, read);
			}
		} catch (IOException e) {
			JMSException e1=new JMSException(e.getLocalizedMessage());
			e1.initCause(e);
			throw e1;
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return message;
	}
}