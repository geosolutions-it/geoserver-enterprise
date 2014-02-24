/* Copyright (c) 2001 - 2013 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package it.geosolutions.geoserver.jms.impl.rest;

import it.geosolutions.geoserver.jms.client.JMSContainer;
import it.geosolutions.geoserver.jms.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.configuration.ConnectionConfiguration;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ReadOnlyConfiguration;
import it.geosolutions.geoserver.jms.configuration.ToggleConfiguration;
import it.geosolutions.geoserver.jms.events.ToggleEvent;
import it.geosolutions.geoserver.jms.events.ToggleType;
import it.geosolutions.geoserver.jms.impl.configuration.ConfigDirConfiguration;

import java.io.IOException;

import org.geoserver.config.ReadOnlyGeoServerLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * 
 * @author carlo cancellieri - geosolutions SAS
 *
 */
public class Controller {

	@Autowired
	public JMSConfiguration config;

	@Autowired
	public ReadOnlyGeoServerLoader loader;

	@Autowired
	public ApplicationContext ctx;

	@Autowired
	JMSContainer container;

	public void setInstanceName(final String name) {
		Assert.notNull(name, "Unable to setup a null name");
		config.putConfiguration(JMSConfiguration.INSTANCE_NAME_KEY, name);
	}

	public void setBrokerURL(final String url) {
		Assert.notNull(url, "Unable to setup a null Broker URL");
		config.putConfiguration(BrokerConfiguration.BROKER_URL_KEY, url);
	}

	public void setReadOnly(final boolean set) {
		loader.setEnabled(set);
		config.putConfiguration(ReadOnlyConfiguration.READ_ONLY_KEY, Boolean
				.valueOf(set).toString());
	}

	public void setConfigDir(final String path) {
		Assert.notNull(path, "Unable to setup a null path");
		config.putConfiguration(ConfigDirConfiguration.CONFIGDIR_KEY, path);
	}

	public void toggle(final boolean switchTo, final ToggleType type) {

		ctx.publishEvent(new ToggleEvent(switchTo, type));

		final String switchToValue = Boolean.valueOf(switchTo).toString();
		if (type.equals(ToggleType.MASTER))
			config.putConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY,
					switchToValue);
		else
			config.putConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY,
					switchToValue);

		// if (switchTo) {
		// // LOGGER.info("The " + type + " toggle is now ENABLED");
		// } else {
		// // LOGGER.warn("The " + type
		// // +
		// //
		// " toggle is now DISABLED no event will be posted/received to/from the broker");
		// // fp.info("Note that the " + type
		// // + " is still registered to the topic destination");
		// }
	}

	public void connectClient(final boolean connect) throws IOException {
		if (connect) {

			if (!container.isRunning()) {
				// .info("Connecting...");
				if (container.connect()) {
					// .info("Now GeoServer is registered with the destination");
					config.putConfiguration(
							ConnectionConfiguration.CONNECTION_KEY,
							Boolean.TRUE.toString());
				} else {
					config.putConfiguration(
							ConnectionConfiguration.CONNECTION_KEY,
							Boolean.FALSE.toString());
					throw new IOException(
							"Connection error: Registration aborted due to a connection problem");
				}

			}
		} else {
			if (container.isRunning()) {
				// LOGGER.info("Disconnecting...");
				if (container.disconnect()) {
					config.putConfiguration(
							ConnectionConfiguration.CONNECTION_KEY,
							Boolean.FALSE.toString());
				} else {
					config.putConfiguration(
							ConnectionConfiguration.CONNECTION_KEY,
							Boolean.TRUE.toString());
					throw new IOException("Disconnection error");
				}
			}
		}
	}

	public void save() throws IOException {
		config.storeConfig();
	}
}
