/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geoserver.catalog.ResourcePool;
import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.data.DataAccessFactoryProducer;
import org.geoserver.feature.FeatureSourceUtils;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.vividsolutions.jts.geom.Envelope;


/**
 * A collecitno of utilties for dealing with GeotTools DataStore.
 *
 * @author Richard Gould, Refractions Research, Inc.
 * @author $Author: cholmesny $ (last modification)
 * @version $Id$
 */
public abstract class DataStoreUtils {
    
    /**
     * logger
     */
    static Logger LOGGER = Logging.getLogger("org.geoserver.data");
    
    /**
     * Uses the standard datastore factory mechanism, but first manipulates the
     * specified parameters so that data dir relative paths gets turned into
     * absolute ones
     * @param params
     * @param sc
     * @return
     * @throws IOException
     * @deprecated as for 2.0.x
     */
    public static DataStore acquireDataStore(Map params, ServletContext sc)
        throws IOException {
        //DJB: changed this for geoserver_data_dir   	
        //String baseDir = sc.getRealPath("/");
        File baseDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        return getDataStore(ResourcePool.getParams(params, baseDir.getAbsolutePath()));
    }
    
    /**
     * Looks up the datastore using the given params, verbatim, and then
     * eventually wraps it into a renaming wrapper so that feature type
     * names are good ones from the wfs point of view (that is, no ":" in the type names)
     * @param params
     * @return
     * @deprecated use {@link #getDataAccess(Map)}
     */
    public static DataStore getDataStore(Map params) throws IOException {
        DataAccess<? extends FeatureType, ? extends Feature> store;
        store = getDataAccess(params);
        if (!(store instanceof DataStore)) {
            return null;
        }
        return (DataStore) store;
    }

    /**
     * Looks up the {@link DataAccess} using the given params, verbatim, and then eventually wraps
     * it into a renaming wrapper so that feature type names are good ones from the wfs point of
     * view (that is, no ":" in the type names)
     * 
     * @param params
     * @return
     */
    public static DataAccess<? extends FeatureType, ? extends Feature> getDataAccess(Map params)
            throws IOException {
        DataAccessFactory factory = aquireFactory(params);
        if (factory == null) {
            return null;
        }

        DataAccess<? extends FeatureType, ? extends Feature> store = factory
                .createDataStore(params);
        if (store == null) {
            return null;
        }

        if (store instanceof DataStore) {
            String[] names = ((DataStore) store).getTypeNames();
            for (int i = 0; i < names.length; i++) {
                if (names[i].indexOf(":") >= 0)
                    return new RetypingDataStore((DataStore) store);
            }
        }
        return store;
    }

    public static Map getParams(Map m) {
        return getParams(m,null);
    }
    
    public static Map getParams(Map m, ServletContext sc) {
        File data_dir = GeoserverDataDirectory.getGeoserverDataDirectory();
        String baseDir = data_dir.getPath();

        return ResourcePool.getParams(m, baseDir);
    }

    /**
     * When loading from DTO use the params to locate factory.
     *
     * <p>
     * bleck
     * </p>
     *
     * @param params
     *
     * @return
     */
    public static DataAccessFactory aquireFactory(Map params) {
        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext();) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory( factory );
            
            if (factory.canProcess(params)) {
                return factory;
            }
        }

        return null;
    }

    /**
     * After user has selected Description can aquire Factory based on
     * display name.
     *
     * <p>
     * Use factory for:
     * </p>
     *
     * <ul>
     * <li>
     * List of Params (attrb name, help text)
     * </li>
     * <li>
     * Checking user's input with factory.canProcess( params )
     * </li>
     * </ul>
     *
     *
     * @param diplayName
     *
     * @return
     */
    public static DataAccessFactory aquireFactory(String displayName) {
        if(displayName == null) {
            return null;
        }
        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext();) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory( factory );
            
            if (displayName.equals(factory.getDisplayName())) {
                return factory;
            }

            if (displayName.equals(factory.getClass().toString())) {
                return factory;
            }
        }

        return null;
    }

    /**
     * Initializes a newly created data store factory by processing the {@link DataStoreFactoryInitializer} 
     * extension point.
     *
     */
    static DataAccessFactory initializeDataStoreFactory( DataAccessFactory factory ) {
        List initializers = GeoServerExtensions.extensions( DataStoreFactoryInitializer.class );
        for ( Iterator i = initializers.iterator(); i.hasNext(); ) {
            DataStoreFactoryInitializer initer = (DataStoreFactoryInitializer) i.next();
            if ( initer.getFactoryClass().isAssignableFrom( factory.getClass() ) ) {
                try {
                    initer.initialize( factory );
                }
                catch( Throwable t ) {
                    String msg = "Error occured processing extension: " + initer.getClass().getName();
                    GeoServerExtensions.LOGGER.log( Level.WARNING, msg, t );
                }
            }
        }
        
        return factory;
    }
    

    /**
     * Utility methods for find param by key
     *
     * @param params DOCUMENT ME!
     * @param key DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static Param find(Param[] params, String key) {
        for (int i = 0; i < params.length; i++) {
            if (key.equalsIgnoreCase(params[i].key)) {
                return params[i];
            }
        }

        return null;
    }

    /**
     * Returns the descriptions for the available DataStores.
     *
     * <p>
     * Arrrg! Put these in the select box.
     * </p>
     *
     * @return Descriptions for user to choose from
     */
    public static List listDataStoresDescriptions() {
        List list = new ArrayList();

        for (Iterator i = getAvailableDataStoreFactories().iterator(); i.hasNext();) {
            DataAccessFactory factory = (DataAccessFactory) i.next();
            initializeDataStoreFactory(factory);
            
            list.add(factory.getDisplayName());
        }

        return list;
    }

    public static Map defaultParams(String description) {
        return defaultParams(aquireFactory(description));
    }

    public static Map defaultParams(DataAccessFactory factory) {
        Map defaults = new HashMap();
        Param[] params = factory.getParametersInfo();

        for (int i = 0; i < params.length; i++) {
            Param param = params[i];
            String key = param.key;
            String value = null;

            //if (param.required ) {
            if (param.sample != null) {
                // Required params may have nice sample values
                //
                value = param.text(param.sample);
            }

            if (value == null) {
                // or not
                value = "";
            }

            //}
            if (value != null) {
                defaults.put(key, value);
            }
        }

        return defaults;
    }

    /**
     * Convert map to real values based on factory Params.
     *
     * <p>
     * The resulting map should still be checked with factory.acceptsMap( map )
     * </p>
     *
     * @param factory
     * @param params
     *
     * @return Map with real values that may be acceptable to Factory
     *
     * @throws IOException DOCUMENT ME!
     */
    public static Map toConnectionParams(DataAccessFactory factory, Map params)
        throws IOException {
        Map map = new HashMap(params.size());

        Param[] info = factory.getParametersInfo();

        // Convert Params into the kind of Map we actually need
        for (Iterator i = params.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();

            Object value = find(info, key).lookUp(params);

            if (value != null) {
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * @deprecated use {@link org.geoserver.feature.FeatureSourceUtils#
     */
    public static Envelope getBoundingBoxEnvelope(FeatureSource<? extends FeatureType, ? extends Feature> fs)
        throws IOException {
        return FeatureSourceUtils.getBoundingBoxEnvelope(fs);
    }
    
    public static Collection<DataAccessFactory> getAvailableDataStoreFactories() {
        List<DataAccessFactory> factories = new ArrayList();
        Iterator<DataAccessFactory> it = DataAccessFinder.getAvailableDataStores();
        while(it.hasNext()) {
            factories.add(it.next());
        }
        
        for (DataAccessFactoryProducer producer : GeoServerExtensions.extensions(DataAccessFactoryProducer.class)) {
            try {
                factories.addAll(producer.getDataStoreFactories());
            }
            catch(Throwable t) {
                LOGGER.log(Level.WARNING, "Error occured loading data access factories. " +
                    "Ignoring producer", t);
            }
        }
    
        return factories;
    }
}
