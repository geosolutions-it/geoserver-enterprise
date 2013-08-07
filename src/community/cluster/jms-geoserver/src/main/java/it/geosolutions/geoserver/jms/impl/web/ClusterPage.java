/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.client.JMSContainer;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ToggleConfiguration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.events.ToggleType;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.web.GeoServerSecuredPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

	protected JMSConfiguration getConfig() {
		return getGeoServerApplication().getBeanOfType(JMSConfiguration.class);
	}

	protected JMSContainer getJMSContainer() {
		return getGeoServerApplication().getBeanOfType(JMSContainer.class);
	}

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ClusterPage.class);

	public ClusterPage() {


		final String producerStatusString = getConfig()
				.getConfiguration(ToggleConfiguration.TOGGLE_PRODUCER_KEY);
		final boolean producerStatus;
		if (producerStatusString != null) {
			producerStatus=Boolean.parseBoolean(producerStatusString);
		} else {
			producerStatus=false;
		}
		// TODO move to the JMS server module
		final CheckBox toggleProducer = new CheckBox("toggleProducer",
				new Model<Boolean>(producerStatus)) {

			@Override
			protected boolean wantOnSelectionChangedNotifications() {
				return true;
			}

			@Override
			protected void onSelectionChanged(Object newSelection) {
				if (newSelection instanceof Boolean) {
					if (LOGGER.isInfoEnabled())
						LOGGER.info("TOGGLE PRODUCER: " + newSelection);
					final ApplicationContext ctx = getGeoServerApplication()
							.getApplicationContext();
					ctx.publishEvent(new ToggleEvent(Boolean.class
							.cast(newSelection), ToggleType.PRODUCER));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleProducer);
		toggleProducer.setEnabled(true);
		toggleProducer.setVisible(true);

		final String consumerStatusString = getConfig()
				.getConfiguration(ToggleConfiguration.TOGGLE_CONSUMER_KEY);
		final boolean consumerStatus;
		if (producerStatusString != null) {
			consumerStatus=Boolean.parseBoolean(consumerStatusString);
		} else {
			consumerStatus=false;
		}
		final CheckBox toggleConsumer = new CheckBox("toggleConsumer",
				new Model<Boolean>(consumerStatus)) {

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
							.cast(newSelection), ToggleType.CONSUMER));
				} else {
					LOGGER.error("The incoming object is not a BOOLEAN");
				}
			}
		};
		add(toggleConsumer);
		toggleConsumer.setEnabled(true);
		toggleConsumer.setVisible(true);

		// form and submit
		final Form form = new Form("form", new CompoundPropertyModel(
				getConfig().getConfigurations()));
		add(form);

		final TextField<String> brokerURL = new TextField<String>("brokerURL");
		form.add(brokerURL);

		final TextField<String> instanceName = new TextField<String>(
				JMSConfiguration.INSTANCE_NAME_KEY);
		form.add(instanceName);

		final TextField<String> topicName = new TextField<String>("topicName");
		form.add(topicName);

		final Button connect = new Button("connect", new StringResourceModel(
				"connect", this, null)) {
			@Override
			public void onSubmit() {
				getJMSContainer().start();
//				setEnabled(false);
//				setVisible(false);
//				dis
			}
		};
//		if (getJMSContainer().isActive()) {
//			connect.setEnabled(false);
////			connect.setVisible(false);
//		} else {
//			connect.setEnabled(true);
////			connect.setVisible(true);
//		}
		form.add(connect);

		final Button disconnect = new Button("disconnect",
				new StringResourceModel("disconnect", this, null)) {
			@Override
			public void onSubmit() {
				getJMSContainer().stop();
//				setEnabled(false);
//				connect.setEnabled(true);
//				setVisible(false);
			}
		};
//		if (getJMSContainer().isActive()) {
//			disconnect.setEnabled(true);
//			disconnect.setVisible(true);
//		} else {
//			disconnect.setEnabled(false);
//			disconnect.setVisible(false);
//		}
		form.add(disconnect);

		final Button save = new Button("save", new StringResourceModel("save",
				this, null)) {
			@Override
			public void onSubmit() {
				try {
					getConfig().storeTempConfig();
				} catch (IOException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
				}
			}
		};
		form.add(save);

		final Button reset = new Button("reset", new StringResourceModel(
				"reset", this, null)) {
			@Override
			public void onSubmit() {
				try {
					getConfig().loadTempConfig();
				} catch (FileNotFoundException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
				} catch (IOException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
				}
			}
		};
		form.add(reset);
	}
}
