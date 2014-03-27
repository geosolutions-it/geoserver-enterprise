/* Copyright (c) 2001 - 2013 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.client.JMSContainer;
import it.geosolutions.geoserver.jms.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import it.geosolutions.geoserver.jms.configuration.EmbeddedBrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ReadOnlyConfiguration;
import it.geosolutions.geoserver.jms.configuration.ToggleConfiguration;
import it.geosolutions.geoserver.jms.configuration.TopicConfiguration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.events.ToggleType;

import java.io.IOException;
import java.util.Properties;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.config.ReadOnlyGeoServerLoader;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

    private final static java.util.logging.Logger LOGGER = Logging.getLogger(ClusterPage.class);

    public ClusterPage() {

        final FeedbackPanel fp = getFeedbackPanel();

        // setup the JMSContainer exception handler
        getJMSContainerExceptionHandler().setFeedbackPanel(fp);
        getJMSContainerExceptionHandler().setSession(fp.getSession());

        fp.setOutputMarkupId(true);

        // form and submit
        final Form<Properties> form = new Form<Properties>("form",
                new CompoundPropertyModel<Properties>(getConfig().getConfigurations()));

        // add broker URL setting
        final TextField<String> brokerURL = new TextField<String>(
                BrokerConfiguration.BROKER_URL_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        brokerURL.setType(String.class);
        form.add(brokerURL);

        // add instance name setting
        final TextField<String> instanceName = new TextField<String>(
                JMSConfiguration.INSTANCE_NAME_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        instanceName.setType(String.class);
        form.add(instanceName);

        // add topic name setting
        final TextField<String> topicName = new TextField<String>(TopicConfiguration.TOPIC_NAME_KEY);
        // https://issues.apache.org/jira/browse/WICKET-2426
        topicName.setType(String.class);
        form.add(topicName);

        // add connection status info
        final TextField<String> connectionInfo = new TextField<String>(
                ConnectionConfiguration.CONNECTION_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        connectionInfo.setType(String.class);

        connectionInfo.setOutputMarkupId(true);
        connectionInfo.setOutputMarkupPlaceholderTag(true);
        connectionInfo.setEnabled(false);
        form.add(connectionInfo);

        final AjaxButton connection = new AjaxButton("connectionB", new StringResourceModel(
                ConnectionConfiguration.CONNECTION_KEY, this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                // the container to use
                final JMSContainer c = getJMSContainer();
                if (c.isRunning()) {
                    fp.info("Disconnecting...");
                    if (c.disconnect()) {
                        fp.info("Succesfully un-registered from the destination topic");
                        fp.warn("You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
                        connectionInfo.getModel().setObject(
                                ConnectionConfigurationStatus.disabled.toString());
                    } else {
                        fp.error("Disconnection error!");
                        connectionInfo.getModel().setObject(
                                ConnectionConfigurationStatus.enabled.toString());
                    }
                } else {
                    fp.info("Connecting...");
                    if (c.connect()) {
                        fp.info("Now GeoServer is registered with the destination");
                        connectionInfo.getModel().setObject(
                                ConnectionConfigurationStatus.enabled.toString());
                    } else {
                        fp.error("Connection error!");
                        fp.error("Registration aborted due to a connection problem");
                        connectionInfo.getModel().setObject(
                                ConnectionConfigurationStatus.disabled.toString());
                    }
                }
                target.addComponent(connectionInfo);
                target.addComponent(fp);
            }

        };
        connection.setOutputMarkupId(true);
        connection.setOutputMarkupPlaceholderTag(true);
        form.add(connection);

        // add MASTER toggle
        addToggle(ToggleConfiguration.TOGGLE_MASTER_KEY, ToggleType.MASTER,
                ToggleConfiguration.TOGGLE_MASTER_KEY, "toggleMasterB", form, fp);

        // add SLAVE toggle
        addToggle(ToggleConfiguration.TOGGLE_SLAVE_KEY, ToggleType.SLAVE,
                ToggleConfiguration.TOGGLE_SLAVE_KEY, "toggleSlaveB", form, fp);

        // add Read Only switch
        final TextField<String> readOnlyInfo = new TextField<String>(
                ReadOnlyConfiguration.READ_ONLY_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        readOnlyInfo.setType(String.class);

        readOnlyInfo.setOutputMarkupId(true);
        readOnlyInfo.setOutputMarkupPlaceholderTag(true);
        readOnlyInfo.setEnabled(false);
        form.add(readOnlyInfo);

        final AjaxButton readOnly = new AjaxButton("readOnlyB", new StringResourceModel(
                ReadOnlyConfiguration.READ_ONLY_KEY, this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                ReadOnlyGeoServerLoader loader = getReadOnlyGeoServerLoader();
                if (loader.isEnabled()) {
                    readOnlyInfo.getModel().setObject("disabled");
                    loader.enable(false);
                } else {
                    readOnlyInfo.getModel().setObject("enabled");
                    loader.enable(true);
                }
                target.addComponent(this.getParent());
            }
        };
        form.add(readOnly);

        final Button save = new Button("saveB", new StringResourceModel("save", this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit() {
                try {
                    getConfig().storeConfig();
                    fp.info("Configuration saved");
                } catch (IOException e) {
                    if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                        LOGGER.severe(e.getLocalizedMessage());
                    fp.error(e.getLocalizedMessage());
                }
            }
        };
        form.add(save);

        // add Read Only switch
        final TextField<String> embeddedBrokerInfo = new TextField<String>(
                EmbeddedBrokerConfiguration.EMBEDDED_BROKER_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        embeddedBrokerInfo.setType(String.class);

        embeddedBrokerInfo.setOutputMarkupId(true);
        embeddedBrokerInfo.setOutputMarkupPlaceholderTag(true);
        embeddedBrokerInfo.setEnabled(false);
        form.add(embeddedBrokerInfo);

        final AjaxButton embeddedBroker = new AjaxButton(
                "embeddedBrokerB",
                new StringResourceModel(EmbeddedBrokerConfiguration.EMBEDDED_BROKER_KEY, this, null)) {
            /** serialVersionUID */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    org.apache.wicket.markup.html.form.Form<?> form) {
                JMSFactory factory = getJMSFactory();
                if (!factory.isEmbeddedBrokerStarted()) {
                    try {
                        if (factory.startEmbeddedBroker(getConfig().getConfigurations())) {
                            embeddedBrokerInfo.getModel().setObject("enabled");
                        }
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                            LOGGER.severe(e.getLocalizedMessage());
                        fp.error(e.getLocalizedMessage());
                    }
                } else {
                    try {
                        if (factory.stopEmbeddedBroker()) {
                            embeddedBrokerInfo.getModel().setObject("disabled");
                        }
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                            LOGGER.severe(e.getLocalizedMessage());
                        fp.error(e.getLocalizedMessage());
                    }
                }
                target.addComponent(this.getParent());
            }
        };
        form.add(embeddedBroker);

        // TODO change status if it is changed due to reset
        // final Button reset = new Button("resetB", new StringResourceModel("reset", this, null)) {
        // @Override
        // public void onSubmit() {
        // try {
        // getConfig().loadTempConfig();
        // fp.info("Configuration reloaded");
        // } catch (FileNotFoundException e) {
        // LOGGER.error(e.getLocalizedMessage(), e);
        // fp.error(e.getLocalizedMessage());
        // } catch (IOException e) {
        // LOGGER.error(e.getLocalizedMessage(), e);
        // fp.error(e.getLocalizedMessage());
        // }
        // }
        // };
        // form.add(reset);

        // add the form
        add(form);

        // add the status monitor
        add(fp);

    }

    // final JMSConfiguration config,
    private void addToggle(final String configKey, final ToggleType type, final String textFieldId,
            final String buttonId, final Form<?> form, final FeedbackPanel fp) {

        final TextField<String> toggleInfo = new TextField<String>(textFieldId);

        // https://issues.apache.org/jira/browse/WICKET-2426
        toggleInfo.setType(String.class);

        toggleInfo.setOutputMarkupId(true);
        toggleInfo.setOutputMarkupPlaceholderTag(true);
        toggleInfo.setEnabled(false);
        form.add(toggleInfo);

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
                final boolean switchTo = !Boolean.parseBoolean(toggleInfo.getModel().getObject());
                final ApplicationContext ctx = getGeoServerApplication().getApplicationContext();
                ctx.publishEvent(new ToggleEvent(switchTo, type));
                // getConfig().putConfiguration(configKey,
                // Boolean.toString(switchTo));
                if (switchTo) {
                    fp.info("The " + type + " toggle is now ENABLED");
                } else {
                    fp.warn("The "
                            + type
                            + " toggle is now DISABLED no event will be posted/received to/from the broker");
                    fp.info("Note that the " + type
                            + " is still registered to the topic destination");
                }
                toggleInfo.getModel().setObject(Boolean.toString(switchTo));
                target.addComponent(toggleInfo);
                target.addComponent(fp);

            }
        };
        toggle.setRenderBodyOnly(false);

        form.add(toggle);

        // add(new Monitor(Duration.seconds(10)));
    }

    protected JMSConfiguration getConfig() {
        return getGeoServerApplication().getBeanOfType(JMSConfiguration.class);
    }

    protected JMSContainer getJMSContainer() {
        return getGeoServerApplication().getBeanOfType(JMSContainer.class);
    }

    protected JMSFactory getJMSFactory() {
        return getGeoServerApplication().getBeanOfType(JMSFactory.class);
    }

    protected ReadOnlyGeoServerLoader getReadOnlyGeoServerLoader() {
        return getGeoServerApplication().getBeanOfType(ReadOnlyGeoServerLoader.class);
    }

    protected JMSContainerHandlerExceptionListenerImpl getJMSContainerExceptionHandler() {
        return getGeoServerApplication().getBeanOfType(
                JMSContainerHandlerExceptionListenerImpl.class);
    }
}
