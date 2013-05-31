/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Facade providing access to the GeoServer configuration.
 * <p>
 * <h3>Note for singletons</h3>
 * Singleton objects must take care not to maintain references to configuration entities. For 
 * instance the following would an error:
 * <pre>
 * class MySingleton {
 * 
 *   ServiceInfo service;
 *   
 *   MySingleton(GeoServer gs) {
 *      this.service = gs.getServiceByName("mySerfvice", ServiceInfo.class); 
 *   }
 *   
 * }
 * </pre>
 * The reason being that when changes occur to the configuration externally (be it through the web
 * ui or restconfig, etc...) any cached configuration objects become stale. So singleton objects
 * should look up configuration objects on demand.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 * 
 * TODO: events
 */
public interface GeoServer {
    
    /**
     * Single centralized lock to be used whenever saving, applying or loading the GeoServer configuration
     */
    public static final Object CONFIGURATION_LOCK = new Object(); 

    /**
     * The configuration data access facade object.
     */
    GeoServerFacade getFacade();
    
    /**
     * The global geoserver configuration.
     * 
     * @uml.property name="configuration"
     * @uml.associationEnd inverse="geoServer:org.geoserver.config.GeoServerInfo"
     */
    GeoServerInfo getGlobal();

    /**
     * Sets the global configuration.
     */
    void setGlobal( GeoServerInfo global );

    /**
     * Returns the global settings configuration.
     * <p>
     * This method will return {@link GeoServerInfo#getSettings()} unless a local workspace is 
     * set. In that case the settings for that workspace will be checked via 
     * {@link #getSettings(WorkspaceInfo)}, and if one exists will be returned. If local workspace
     * settings do not exist the global settings ({@link GeoServerInfo#getSettings()}) are returned.
     * </p>
     */
    SettingsInfo getSettings();

    /**
     * The settings configuration for the specified workspoace, or <code>null</code> if non exists.
     */
    SettingsInfo getSettings(WorkspaceInfo workspace);

    /**
     * Adds a settings configuration for the specified workspace.
     */
    void add(SettingsInfo settings);

    /**
     * Saves the settings configuration for the specified workspace.
     */
    void save(SettingsInfo settings);

    /**
     * Removes the settings configuration for the specified workspace.
     */
    void remove(SettingsInfo settings);

    /**
     * The logging configuration.
     */
    LoggingInfo getLogging();
    
    /**
     * Sets logging configuration.
     */
    void setLogging( LoggingInfo logging );
    
    /**
     * The catalog.
     */
    Catalog getCatalog();
    
    /**
     * Sets the catalog.
     */
    void setCatalog( Catalog catalog );
    
    /**
     * Saves the global geoserver configuration after modification.
     */
    void save(GeoServerInfo geoServer);

    /**
     * Saves the logging configuration.
     */
    void save(LoggingInfo logging);
    
    /**
     * Adds a service to the configuration.
     */
    void add(ServiceInfo service);

    /**
     * Removes a service from the configuration.
     */
    void remove(ServiceInfo service);

    /**
     * Saves a service that has been modified.
     */
    void save(ServiceInfo service);

    /**
     * GeoServer services.
     * 
     * @uml.property name="services"
     * @uml.associationEnd multiplicity="(0 -1)"
     *                     inverse="geoServer1:org.geoserver.config.ServiceInfo"
     */
    Collection<? extends ServiceInfo> getServices();

    /**
     * GeoServer services local to the specified workspace.
     *
     * @param workspace THe workspace containing the service.
     */
    Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace);

    /**
     * GeoServer services filtered by class.
     * <p>
     * 
     * </p>
     * @param clazz
     *                The class of the services to return.
     */
    <T extends ServiceInfo> T getService(Class<T> clazz);

    /**
     * GeoServer services filtered by workspace and class.
     *
     * @param workspace The containing workspace.
     * @param clazz The class of the services to return.
     */
    <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz);

    /**
     * Looks up a service by id.
     * 
     * @param id
     *                The id of the service.
     * @param clazz The type of the service.
     * 
     * @return The service with the specified id, or <code>null</code> if no
     *         such service coud be found.
     */
    <T extends ServiceInfo> T getService(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     * 
     * @param name The name of the service.
     * @param clazz The type of the service.
     * 
     * @return The service with the specified name or <code>null</code> if no
     *         such service could be found.
     */
    <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz );

    /**
     * Looks up a service by name, local to a specific workspace.
     * 
     * @param name THe workspace.
     * @param name The name of the service.
     * @param clazz The type of the service.
     * 
     * @return The service with the specified name or <code>null</code> if no
     *         such service could be found within the workspace.
     */
    <T extends ServiceInfo> T getServiceByName(WorkspaceInfo workspace, String name, Class<T> clazz );

    /**
     * The factory used to create configuration object.
     * 
     * @uml.property name="factory"
     * @uml.associationEnd inverse="geoServer:org.geoserver.config.GeoServerFactory"
     */
    GeoServerFactory getFactory();

    /**
     * Sets the factory used to create configuration object.
     * 
     * @uml.property name="factory"
     * @uml.associationEnd inverse="geoServer:org.geoserver.config.GeoServerFactory"
     */
    void setFactory(GeoServerFactory factory);
    
    /**
     * Adds a listener to the configuration.
     */
    void addListener( ConfigurationListener listener );

    /**
     * Removes a listener from the configuration.
     */
    void removeListener( ConfigurationListener listener );
    
    /**
     * Returns all configuration listeners.
     * <p>
     * This list should not be modified by client code.
     * </p>
     */
    Collection<ConfigurationListener> getListeners();
    
    /**
     * Fires the event for the global configuration being modified.
     * <p> 
     * This method should not be called by client code. It is meant to be called
     * internally by the configuration subsystem.
     * </p>
     */
    void fireGlobalModified(GeoServerInfo global, List<String> propertyNames, List oldValues, List newValues);

    /**
     * Fires the event for a settings configuration being modified.
     * <p> 
     * This method should not be called by client code. It is meant to be called
     * internally by the configuration subsystem.
     * </p>
     */
    void fireSettingsModified(SettingsInfo global, List<String> propertyNames, List oldValues, List newValues);

    /**
     * Fires the event for the logging configuration being modified.
     * <p> 
     * This method should not be called by client code. It is meant to be called
     * internally by the configuration subsystem.
     * </p>
     */
    void fireLoggingModified(LoggingInfo logging, List<String> propertyNames, List oldValues, List newValues);
    
    /**
     * Fires the event for a service configuration being modified.
     * <p> 
     * This method should not be called by client code. It is meant to be called
     * internally by the configuration subsystem.
     * </p>
     */
    void fireServiceModified(ServiceInfo service, List<String> propertyNames, List oldValues, List newValues);

    /**
     * Disposes the configuration. 
     */
    void dispose();
    
    /**
     * Clears up all of the caches inside GeoServer forcing reloading of all information
     * besides the configuration itself
     */
    void reset();
    
    /**
     * Clears up all of the caches as well as the configuration information
     * @throws Exception 
     */
    void reload() throws Exception;
    
}
