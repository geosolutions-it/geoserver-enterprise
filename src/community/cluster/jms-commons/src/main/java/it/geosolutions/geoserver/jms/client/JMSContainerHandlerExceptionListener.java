package it.geosolutions.geoserver.jms.client;

import org.springframework.jms.listener.DefaultMessageListenerContainer;

public interface JMSContainerHandlerExceptionListener {
	
	/**
	 * @see {@link DefaultMessageListenerContainer#handleListenerSetupFailure(Throwable, boolean)}
	 * @param ex - the incoming exception to handle
	 * @param alreadyRecovered - true if the error is already recovered by a different handler
	 */
	public void handleListenerSetupFailure(Throwable ex,
			boolean alreadyRecovered);
}
