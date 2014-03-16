package it.geosolutions.geoserver.jms.impl;

import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.management.JMSStatsImpl;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.IdGenerator;

/**
 * This class was implemented as workaround to a possible ActiveMQ issue with
 * the Trasport handling. Here we create a simple wrapper which will take care
 * of transaction close using the connection.
 * 
 * @author carlo cancellieri
 * 
 */
public class TransactionHandlerConnectionFactory extends ActiveMQConnectionFactory {

	public TransactionHandlerConnectionFactory(String brokerURL) {
		super(brokerURL);
	}

	protected ActiveMQConnection createActiveMQConnection(Transport transport,
			JMSStatsImpl stats) throws Exception {
		return new TransactionHandlingConnection(transport,
				getClientIdGenerator(), getConnectionIdGenerator(), stats);
	}

	/**
	 * This implementation will take care of the transport stop
	 * 
	 * @author carlo cancellieri
	 * 
	 */
	class TransactionHandlingConnection extends ActiveMQConnection {
		protected TransactionHandlingConnection(Transport transport,
				IdGenerator clientIdGenerator,
				IdGenerator connectionIdGenerator, JMSStatsImpl factoryStats)
				throws Exception {
			super(transport, clientIdGenerator, connectionIdGenerator,
					factoryStats);
		}

		@Override
		public void close() throws JMSException {
			try {
				this.getTransport().stop();
			} catch (Exception e) {
				final JMSException ex = new JMSException(
						e.getLocalizedMessage());
				ex.initCause(e.getCause());
				throw ex;
			}
			super.close();
		}

	}
}
