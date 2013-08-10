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

import java.awt.TextArea;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
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

	protected JMSContainerHandlerExceptionListenerImpl getJMSContainerExceptionHandler() {
		return getGeoServerApplication().getBeanOfType(
				JMSContainerHandlerExceptionListenerImpl.class);
	}

	private final static Logger LOGGER = LoggerFactory
			.getLogger(ClusterPage.class);

	public ClusterPage() {

		final FeedbackPanel fp = getFeedbackPanel();

		// setup the JMSContainer exception handler
		getJMSContainerExceptionHandler().setFeedbackPanel(fp);
		getJMSContainerExceptionHandler().setSession(fp.getSession());

		fp.setOutputMarkupId(true);

		// form and submit
		final Form form = new Form("form", new CompoundPropertyModel(
				getConfig().getConfigurations()));

		final TextField<String> brokerURL = new TextField<String>("brokerURL");
		form.add(brokerURL);

		final TextField<String> instanceName = new TextField<String>(
				JMSConfiguration.INSTANCE_NAME_KEY);
		form.add(instanceName);

		final TextField<String> topicName = new TextField<String>("topicName");
		form.add(topicName);

		// TODO
		// final String connectionStatusString = null;//
		// getConfig().getConfiguration(null);
		final Model<String> connectionStatusModel;
		// if (connectionStatusString != null
		// && Boolean.parseBoolean(connectionStatusString)) {
		// connectionStatusModel = new Model<String>(connectionStatusString);
		// } else {
		connectionStatusModel = new Model<String>("true");
		// }

		final TextField<String> connectionInfo = new TextField<String>(
				"connectionInfo", connectionStatusModel);
		connectionInfo.setOutputMarkupId(true);
		connectionInfo.setOutputMarkupPlaceholderTag(true);
		connectionInfo.setEnabled(false);
		form.add(connectionInfo);

		final AjaxButton connection = new AjaxButton("connection") {
			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				JMSContainer c = getJMSContainer();
				boolean switchTo = !Boolean.parseBoolean(connectionStatusModel
						.getObject());
				// switchTo=!switchTo;

				if (c.isRunning()) {
					fp.info("Disconnecting...");
					c.stop();
					fp.info("Disconnected");

					for (int rep = 3; rep > 0; --rep) {
						fp.info("Waiting for connection shutdown...(" + rep
								+ ")");
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							fp.warn(e.getLocalizedMessage());
							LOGGER.error(e.getLocalizedMessage(), e);
						}
						fp.info("Checking the connection...");
						if (!c.isRegisteredWithDestination()) {
							fp.info("Succesfully un-registered from the destination topic");
							fp.warn("You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
							switchTo = false;
							break;
						} else {
							switchTo = true;
						}
					}
				} else {
					fp.info("Connecting...");
					c.start();
					fp.info("Connected...");
					for (int rep = 3; rep > 0; --rep) {
						fp.info("Waiting for a connection...(" + rep + ")");
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							fp.warn(e.getLocalizedMessage());
							LOGGER.error(e.getLocalizedMessage(), e);
						}
						fp.info("Checking the connection...");
						if (c.isRegisteredWithDestination()) {
							fp.info("Now the consumer is registered with the topic destination");
							switchTo = true;
							break;
						} else {
							fp.error("Impossible to register with destination, please check the broker.");
							switchTo = false;
						}
					}
				}
				connectionStatusModel.setObject(Boolean.toString(switchTo));
				getConfig().putConfiguration(
						ToggleConfiguration.TOGGLE_PRODUCER_KEY,
						Boolean.toString(switchTo));
				target.addComponent(connectionInfo);
				target.addComponent(fp);
			}

		};
		connection.setOutputMarkupId(true);
		connection.setOutputMarkupPlaceholderTag(true);
		form.add(connection);

		addToggle(ToggleConfiguration.TOGGLE_PRODUCER_KEY, ToggleType.PRODUCER,
				"toggleProducerInfo", "toggleProducer", form, fp);

		// final String producerStatusString = getConfig().getConfiguration(
		// ToggleConfiguration.TOGGLE_PRODUCER_KEY);
		// final Model<Boolean> producerStatusModel;
		// if (producerStatusString != null) {
		// producerStatusModel = new Model<Boolean>(
		// Boolean.parseBoolean(producerStatusString));
		// } else {
		// producerStatusModel = new Model<Boolean>(false);
		// }
		//
		// final TextField<Boolean> toggleProducerInfo = new TextField<Boolean>(
		// "toggleProducerInfo", producerStatusModel);
		// form.add(toggleProducerInfo);
		//
		// final AjaxButton toggleProducer = new AjaxButton("toggleProducer") {
		// private static final long serialVersionUID = 1L;
		//
		// @Override
		// protected void onError(AjaxRequestTarget target,
		// org.apache.wicket.markup.html.form.Form<?> form) {
		// fp.error("ERROR");
		//
		// target.addComponent(fp);
		// };
		//
		// @Override
		// protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
		// final ApplicationContext ctx = getGeoServerApplication()
		// .getApplicationContext();
		// final IModel<Boolean> model = toggleProducerInfo.getModel();
		// final boolean switchTo = !Boolean.parseBoolean(model
		// .getObject().toString());
		// ctx.publishEvent(new ToggleEvent(switchTo, ToggleType.PRODUCER));
		// getConfig().putConfiguration(
		// ToggleConfiguration.TOGGLE_PRODUCER_KEY,
		// Boolean.toString(switchTo));
		// producerStatusModel.setObject(switchTo);
		// if (switchTo) {
		// fp.info("The MASTER (producer) toggle is now ENABLED");
		// } else {
		// fp.warn("The MASTER (producer) toggle is now DISABLED no event will be posted to the broker");
		// fp.info("Note that the MASTER is still registered to the topic destination");
		// }
		// model.setObject(switchTo);
		// target.addComponent(this);
		// target.addComponent(fp);
		//
		// }
		// };
		// toggleProducer.setRenderBodyOnly(false);
		// // toggleProducer.setLabel(new StringResourceModel("toggleProducer",
		// // new Model<Boolean>(producerStatus)));
		// // Enabled(true);
		// // toggleProducer.setVisible(true);
		// form.add(toggleProducer);
		//
		// final String consumerStatusString = getConfig().getConfiguration(
		// ToggleConfiguration.TOGGLE_CONSUMER_KEY);
		// final boolean consumerStatus;
		// if (producerStatusString != null) {
		// consumerStatus = Boolean.parseBoolean(consumerStatusString);
		// } else {
		// consumerStatus = false;
		// }

		addToggle(ToggleConfiguration.TOGGLE_CONSUMER_KEY, ToggleType.CONSUMER,
				"toggleConsumerInfo", "toggleConsumer", form, fp);

		// final LabelModel toggleConsumerLabel = new
		// LabelModel("toggleProducer");
		// final PropertyModel<String> toggleConsumerModel = new
		// PropertyModel<String>(
		// toggleConsumerLabel, "label");
		//
		// final AjaxLink<String> toggleConsumer = new AjaxLink<String>(
		// "toggleConsumer", toggleConsumerModel) {
		//
		// private static final long serialVersionUID = -7224427619057260577L;
		//
		// @Override
		// public void onClick(AjaxRequestTarget target) {
		// final ApplicationContext ctx = getGeoServerApplication()
		// .getApplicationContext();
		// ctx.publishEvent(new ToggleEvent(consumerStatus,
		// ToggleType.CONSUMER));
		// getConfig().putConfiguration(
		// ToggleConfiguration.TOGGLE_CONSUMER_KEY,
		// Boolean.toString(consumerStatus));
		// if (consumerStatus) {
		// fp.info("The SLAVE (consumer) toggle is now ENABLED");
		// } else {
		// fp.warn("The SLAVE (consumer) toggle is now DISABLED all the incoming events will be discarded");
		// fp.info("Note that the SLAVE is still registered to the topic destination");
		// }
		//
		// target.addComponent(this);
		// target.addComponent(fp);
		// }
		//
		// };
		// // toggleConsumer.set(toggleConsumerModel);
		// // toggleConsumer.setEnabled(true);
		// // toggleConsumer.setVisible(true);
		// toggleConsumer.setOutputMarkupPlaceholderTag(true);
		// toggleConsumer.setOutputMarkupId(true);
		// form.add(toggleConsumer);

		final Button save = new Button("save", new StringResourceModel("save",
				this, null)) {
			@Override
			public void onSubmit() {
				try {
					getConfig().storeTempConfig();
					fp.info("Configuration saved");
				} catch (IOException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
					fp.error(e.getLocalizedMessage());
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
					fp.info("Configuration reloaded");
				} catch (FileNotFoundException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
					fp.error(e.getLocalizedMessage());
				} catch (IOException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
					fp.error(e.getLocalizedMessage());
				}
			}
		};
		form.add(reset);

		// add the form
		add(form);

		// add the status monitor
		add(fp);

	}

	// final JMSConfiguration config,
	private void addToggle(final String configKey, final ToggleType type,
			final String textFieldId, final String buttonId,
			final Form<?> form, final FeedbackPanel fp) {
		// final String producerStatusString = getConfig().getConfiguration(
		// configKey);
		final Model<String> producerStatusModel;
		// if (producerStatusString != null
		// && Boolean.parseBoolean(null)) { // TODO
		// producerStatusModel = new Model<String>(producerStatusString);
		// } else {
		producerStatusModel = new Model<String>("true");
		// }

		final TextField<String> toggleProducerInfo = new TextField<String>(
				textFieldId, producerStatusModel);
		toggleProducerInfo.setOutputMarkupId(true);
		toggleProducerInfo.setOutputMarkupPlaceholderTag(true);
		toggleProducerInfo.setEnabled(false);
		form.add(toggleProducerInfo);

		final AjaxButton toggle = new AjaxButton(buttonId) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				fp.error("ERROR");

				target.addComponent(fp);
			};

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				final boolean switchTo = !Boolean
						.parseBoolean(producerStatusModel.getObject());
				final ApplicationContext ctx = getGeoServerApplication()
						.getApplicationContext();
				ctx.publishEvent(new ToggleEvent(switchTo, type));
//				getConfig().putConfiguration(configKey,
//						Boolean.toString(switchTo));
				if (switchTo) {
					fp.info("The " + type + " toggle is now ENABLED");
				} else {
					fp.warn("The "
							+ type
							+ " toggle is now DISABLED no event will be posted to the broker");
					fp.info("Note that the " + type
							+ " is still registered to the topic destination");
				}
				producerStatusModel.setObject(Boolean.toString(switchTo));
				target.addComponent(toggleProducerInfo);
				target.addComponent(fp);

			}
		};
		toggle.setRenderBodyOnly(false);
		// toggleProducer.setLabel(new StringResourceModel("toggleProducer",
		// new Model<Boolean>(producerStatus)));
		// Enabled(true);
		// toggleProducer.setVisible(true);
		form.add(toggle);

	}

	private static class LabelModel implements Serializable {
		private String label;

		public LabelModel(String val) {
			label = val;
		}

		public void setLabel(String lab) {
			label = lab;
		}

		public String getLabel() {
			return label;
		}
	}
}
