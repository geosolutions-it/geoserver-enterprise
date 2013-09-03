/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.impl.handlers.catalog;

import it.geosolutions.geoserver.jms.events.ToggleSwitch;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NullArgumentException;
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
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class JMSCatalogPostModifyEventHandler extends JMSCatalogEventHandler {
	
	private static final long serialVersionUID = -6421638425464046598L;

	final static java.util.logging.Logger LOGGER = Logging
			.getLogger(JMSCatalogPostModifyEventHandler.class);
	
	private final Catalog catalog;
	private final ToggleSwitch producer;

	public JMSCatalogPostModifyEventHandler(Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer){
		super(xstream,clazz);
		this.catalog=catalog;
		this.producer=producer;
	}

	@Override
	public boolean synchronize(CatalogEvent event) throws Exception {
		if (event==null) {
			throw new NullArgumentException("Incoming object is null");
		}
		try {
			
			if (event instanceof CatalogPostModifyEvent){
				final CatalogPostModifyEvent postModEv=((CatalogPostModifyEvent)event);
				
				producer.disable();
				
				postModify(catalog, postModEv);
				
			} else {
				// incoming object not recognized
				if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
					LOGGER.severe("Unrecognized event type");
				return false;
			}

		} catch (Exception e) {
			if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
				LOGGER.severe(this.getClass()+" is unable to synchronize the incoming event: "+event);
			throw e;
		} finally {
			// re enable the producer
			producer.enable();
		}
		return true;
	}
	
	/**
	 * 
	 * @param catalog
	 * @param modifyEv
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * 
	 * TODO: synchronization on catalog object
	 */
	protected static void postModify(final Catalog catalog, CatalogPostModifyEvent modifyEv)
			throws IllegalAccessException, InvocationTargetException {
		
		final CatalogInfo info=modifyEv.getSource();

		if (info instanceof LayerGroupInfo) {
			
			final LayerGroupInfo localizedObject = CatalogUtils.localizeLayerGroup((LayerGroupInfo) info, catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));

	
		} else if (info instanceof LayerInfo) {
	
			final LayerInfo localizedObject=CatalogUtils.localizeLayer((LayerInfo) info, catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
			
		} else if (info instanceof MapInfo) {
	
			final MapInfo localizedObject = CatalogUtils.localizeMapInfo((MapInfo) info,catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
			
		} else if (info instanceof NamespaceInfo) {
	
			final NamespaceInfo localizedObject=CatalogUtils.localizeNamespace((NamespaceInfo) info, catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
			
		} else if (info instanceof StoreInfo) {
	
			final StoreInfo localizedObject=CatalogUtils.localizeStore((StoreInfo)info,catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
	
		} else if (info instanceof ResourceInfo) {
			
			final ResourceInfo localizedObject=CatalogUtils.localizeResource((ResourceInfo)info,catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
	
		} else if (info instanceof StyleInfo) {
	
			final StyleInfo localizedObject = CatalogUtils.localizeStyle((StyleInfo) info, catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
	
		} else if (info instanceof WorkspaceInfo) {
			
			final WorkspaceInfo localizedObject= CatalogUtils.localizeWorkspace((WorkspaceInfo) info, catalog);
			catalog.firePostModified(ModificationProxy.unwrap(localizedObject));
	
		} else if (info instanceof CatalogInfo) {

			if (LOGGER.isLoggable(java.util.logging.Level.WARNING)){
				LOGGER.warning("info - ID: "+info.getId()+" toString: "+info.toString());
			}
			
		} else {
			if (LOGGER.isLoggable(java.util.logging.Level.WARNING)){
				LOGGER.warning("info - ID: "+info.getId()+" toString: "+info.toString());
			}
			throw new IllegalArgumentException("Bad incoming object: "+info.toString());
		}
	}


}
