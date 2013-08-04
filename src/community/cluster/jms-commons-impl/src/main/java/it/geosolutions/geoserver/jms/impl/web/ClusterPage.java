/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.configuration.Configuration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.impl.configuration.TopicConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.GeoServer;
import org.geoserver.web.GeoServerSecuredPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

	protected Configuration getConfig() {
		return getGeoServerApplication().getBeanOfType(Configuration.class);
	}

	// TODO move into server module
	protected boolean isProducer() {
		return getGeoServerApplication().getBean("JMSCatalogListener") != null;
	}

	// TODO move into client module
	protected boolean isConsumer() {
		return getGeoServerApplication().getBean("JMSQueueListener") != null;
	}

	public IModel getGeoServerModel() {
		return new LoadableDetachableModel() {
			public Object load() {
				return getGeoServerApplication().getGeoServer();
			}
		};
	}

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ClusterPage.class);

	public ClusterPage() {

		final IModel geoServerModel = getGeoServerModel();
		GeoServer gs = (GeoServer) geoServerModel.getObject();

		// TODO move to the JMS server module
		boolean isProducer = isProducer();
		final CheckBox toggleProducer = new CheckBox("toggleProducer",
				new Model<Boolean>(isProducer)) {

			@Override
			protected boolean wantOnSelectionChangedNotifications() {
				return true;
			}

			@Override
			protected void onSelectionChanged(Object newSelection) {
				if (newSelection instanceof Boolean) {
					// TODO applicationevent
					if (LOGGER.isInfoEnabled())
						LOGGER.info("TOGGLE PRODUCER: " + newSelection);
					final ApplicationContext ctx = getGeoServerApplication()
							.getApplicationContext();
					ctx.publishEvent(new ToggleEvent(
							Boolean.class.cast(newSelection)));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleProducer);
		toggleProducer.setEnabled(isProducer);
		if (!isProducer) {
			toggleProducer.setVisible(isProducer);
		}

		boolean isConsumer = isConsumer();
		final CheckBox toggleConsumer = new CheckBox("toggleConsumer",
				new Model<Boolean>(isConsumer)) {

			@Override
			protected boolean wantOnSelectionChangedNotifications() {
				return true;
			}

			@Override
			protected void onSelectionChanged(Object newSelection) {
				if (newSelection instanceof Boolean) {
					// TODO applicationevent
					if (LOGGER.isInfoEnabled())
						LOGGER.info("TOGGLE CONSUMER: " + newSelection);
					// final ApplicationContext
					// ctx=getGeoServerApplication().getApplicationContext();
					// ctx.publishEvent(new
					// ToggleProducer.ToggleEvent(Boolean.class.cast(newSelection)));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleConsumer);
		// disable consumer is not supported
		toggleConsumer.setEnabled(false);

		// form and submit
		Form form = new Form("form", new CompoundPropertyModel(getConfig()
				.getConfigurations()));
		add(form);

		TextField<String> instanceName = new TextField<String>(
				Configuration.INSTANCE_NAME_KEY);
		form.add(instanceName);

		TextField<String> topicName = new TextField<String>(
				TopicConfiguration.TOPIC_NAME_KEY);
		form.add(topicName);

		Button submit = new Button("submit", new StringResourceModel("submit",
				this, null)) {
			@Override
			public void onSubmit() {
				try {
					getConfig().storeTempConfig();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		form.add(submit);

		Button cancel = new Button("cancel") {
			@Override
			public void onSubmit() {
				try {
					getConfig().loadTempConfig();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		form.add(cancel);
	}
}
