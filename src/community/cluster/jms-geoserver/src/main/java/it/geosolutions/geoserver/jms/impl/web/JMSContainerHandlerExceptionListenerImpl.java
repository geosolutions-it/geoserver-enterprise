/* Copyright (c) 2001 - 2013 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.client.JMSContainerHandlerExceptionListener;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * 
 * @author carlo cancellieri - geosolutions sas
 *
 */
public class JMSContainerHandlerExceptionListenerImpl implements
		JMSContainerHandlerExceptionListener {

	private FeedbackPanel fp;
	private Session session;

	public JMSContainerHandlerExceptionListenerImpl() {
	}

	public void setFeedbackPanel(FeedbackPanel fp) {
		this.fp = fp;
	}

	public void setSession(Session s) {
		if (session != null) {
			synchronized (this.session) {
				this.session = s;
			}
		} else {
			synchronized (s) {
				this.session = s;
			}
		}
	}

	//
	// public void setRequestCycle(RequestCycle rc) {
	// if (session != null) {
	// synchronized (this.rc) {
	// this.rc = rc;
	// }
	// } else {
	// synchronized (rc) {
	// this.rc = rc;
	// }
	// }
	// }

	@Override
	public void handleListenerSetupFailure(Throwable ex,
			boolean alreadyRecovered) {
		if (session != null) {
			synchronized (session) {
				if (session.isSessionInvalidated()) {
					return; // skip
				}
				Session.set(session);

				if (fp != null) {
					if (alreadyRecovered) {
						fp.warn("There was an error which seems already fixed: "
								+ ex.getLocalizedMessage());
					} else {
						fp.error(ex.getLocalizedMessage());

					}
				}
			}
		}

	}

}
