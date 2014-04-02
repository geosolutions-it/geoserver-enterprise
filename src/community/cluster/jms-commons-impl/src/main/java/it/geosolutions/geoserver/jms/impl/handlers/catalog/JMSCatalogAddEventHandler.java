/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.events.ToggleProducer;

import java.lang.reflect.InvocationTargetException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Handler for CatalogAddEvent.
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSCatalogAddEventHandler extends JMSCatalogEventHandler {
	private static final long serialVersionUID = -6421638425464046597L;

	final static Logger LOGGER = LoggerFactory
			.getLogger(JMSCatalogAddEventHandler.class);

	private final Catalog catalog;
	private final ToggleProducer producer;

	public JMSCatalogAddEventHandler(Catalog catalog, XStream xstream,
			Class clazz, ToggleProducer producer) {
		super(xstream, clazz);
		this.catalog = catalog;
		this.producer = producer;
	}

	@Override
	public boolean synchronize(CatalogEvent event) throws Exception {
		if (event == null) {
			throw new IllegalArgumentException("Incoming object is null");
		}
		try {
			if (event instanceof CatalogAddEvent) {
				final CatalogAddEvent addEv = ((CatalogAddEvent) event);
				
				// get the source from the incoming event
				final CatalogInfo info = addEv.getSource();
				// disable the producer to avoid recursion
				producer.disable();
				
				// add the incoming CatalogInfo to the local catalog
				JMSCatalogAddEventHandler.add(catalog, info);
			} else {
				// incoming object not recognized
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unrecognized event type");
				return false;
			}

		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(this.getClass()+" is unable to synchronize the incoming event: "+event);
			throw e;
		} finally {
			// re enable the producer
			producer.enable();
		}
		return true;
	}

	private static void add(final Catalog catalog, CatalogInfo info)
			throws IllegalAccessException, InvocationTargetException {

		if (info instanceof LayerGroupInfo) {

			final LayerGroupInfo deserObject = CatalogUtils.localizeLayerGroup(
					(LayerGroupInfo) info, catalog);
			catalog.add(deserObject);

		} else if (info instanceof LayerInfo) {

			final LayerInfo layer = CatalogUtils.localizeLayer(
					(LayerInfo) info, catalog);
			catalog.add(layer);

		} else if (info instanceof MapInfo) {

			final MapInfo localObject = CatalogUtils.localizeMapInfo(
					(MapInfo) info, catalog);
			catalog.add(localObject);

		} else if (info instanceof NamespaceInfo) {

			final NamespaceInfo namespace = CatalogUtils.localizeNamespace(
					(NamespaceInfo) info, catalog);
			catalog.add(namespace);

		} else if (info instanceof StoreInfo) {

			StoreInfo store = CatalogUtils.localizeStore((StoreInfo) info,
					catalog);
			catalog.add(store);

		} else if (info instanceof ResourceInfo) {

			final ResourceInfo resource = CatalogUtils.localizeResource(
					(ResourceInfo) info, catalog);
			catalog.add(resource);

		} else if (info instanceof StyleInfo) {

			final StyleInfo deserializedObject = CatalogUtils.localizeStyle(
					(StyleInfo) info, catalog);
			catalog.add(deserializedObject);

		} else if (info instanceof WorkspaceInfo) {

			final WorkspaceInfo workspace = CatalogUtils.localizeWorkspace(
					(WorkspaceInfo) info, catalog);
			catalog.add(workspace);

		} else if (info instanceof CatalogInfo) {
			// TODO may we don't want to send this empty message!
			// TODO check the producer
			// DO NOTHING
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("info - ID: " + info.getId() + " toString: "
						+ info.toString());
			}
		} else {
			throw new IllegalArgumentException("Bad incoming object: "
					+ info.getClass());
		}
	}

}
