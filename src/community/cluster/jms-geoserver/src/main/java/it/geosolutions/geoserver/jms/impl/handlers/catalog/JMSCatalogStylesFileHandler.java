/*
 * Copyright 2011 GeoSolutions SAS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.configuration.ReadOnlyConfiguration;
import it.geosolutions.geoserver.jms.impl.handlers.DocumentFile;
import it.geosolutions.geoserver.jms.impl.handlers.DocumentFileHandler;

import java.io.File;

import org.apache.commons.lang.NullArgumentException;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSCatalogStylesFileHandler extends DocumentFileHandler {
	private final Catalog catalog;

	private JMSConfiguration config;

	public JMSCatalogStylesFileHandler(Catalog catalog, XStream xstream,
			Class clazz) {
		super(xstream, clazz);
		this.catalog = catalog;
	}

	public void setConfig(JMSConfiguration config) {
		this.config = config;
	}

	@Override
	public boolean synchronize(DocumentFile event) throws Exception {
		if (event == null) {
			throw new NullArgumentException("Incoming object is null");
		}
		if (config == null) {
			throw new IllegalStateException("Unable to load configuration");
		} else if (!ReadOnlyConfiguration.isReadOnly(config)) {
			try {
				GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
				final String fileName = File.separator + "styles"
						+ File.separator + event.getPath().getName();
				final File file = new File(loader.getBaseDirectory().getCanonicalPath(),
						fileName);
				event.writeTo(file);
				return true;
			} catch (Exception e) {
				if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
					LOGGER.severe(this.getClass()
							+ " is unable to synchronize the incoming event: "
							+ event);
				throw e;
			}
		}
		return true;
	}

}
