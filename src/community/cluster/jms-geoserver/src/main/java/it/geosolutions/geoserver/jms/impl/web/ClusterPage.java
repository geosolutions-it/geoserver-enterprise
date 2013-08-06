/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.events.ToggleSwitch;
import it.geosolutions.geoserver.jms.events.ToggleType;

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
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

	protected JMSConfiguration getConfig() {
		return getGeoServerApplication().getBeanOfType(JMSConfiguration.class);
	}

//	// TODO move into server module
//	protected boolean isProducer() {
//		return getGeoServerApplication().getBean("JMSCatalogListener") != null;
//	}
//
//	// TODO move into client module
//	protected boolean isConsumer() {
//		return getGeoServerApplication().getBean("JMSQueueListener") != null;
//	}

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
		final CheckBox toggleProducer = new CheckBox("toggleProducer",
				new Model<Boolean>()) {

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
					ctx.publishEvent(new ToggleEvent(Boolean.class
							.cast(newSelection),ToggleType.PRODUCER));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleProducer);
		toggleProducer.setEnabled(true);
		toggleProducer.setVisible(true);

		final CheckBox toggleConsumer = new CheckBox("toggleConsumer",
				new Model<Boolean>()) {

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
					final ApplicationContext ctx = getGeoServerApplication()
							.getApplicationContext();
					ctx.publishEvent(new ToggleEvent(Boolean.class
							.cast(newSelection),ToggleType.CONSUMER));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleConsumer);
		toggleConsumer.setEnabled(true);
		toggleConsumer.setVisible(true);

		// form and submit
		Form form = new Form("form", new CompoundPropertyModel(getConfig()
				.getConfigurations()));
		add(form);

		TextField<String> instanceName = new TextField<String>(
				JMSConfiguration.INSTANCE_NAME_KEY);
		form.add(instanceName);

		TextField<String> topicName = new TextField<String>("topicName");
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
